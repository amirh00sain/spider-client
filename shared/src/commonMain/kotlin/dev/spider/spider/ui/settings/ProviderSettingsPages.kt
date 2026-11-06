package dev.spider.spider.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.spider.spider.data.config.ConfigRepository
import dev.spider.spider.domain.config.ProfileSource
import dev.spider.spider.domain.config.VpeenLocation
import dev.spider.spider.domain.model.AppSettings
import dev.spider.spider.shared.resources.Res
import dev.spider.spider.shared.resources.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

private sealed interface FirefoxVpnAction {
    data class SignIn(val email: String, val password: String) : FirefoxVpnAction
    data class Verify(val signInId: String, val value: String) : FirefoxVpnAction
    data class SetLocation(val country: String) : FirefoxVpnAction
    data object Refresh : FirefoxVpnAction
}

private fun Throwable.rethrowIfCancelled() {
    if (this is CancellationException) throw this
}

@Composable
internal fun VpeenSettingsPage(
    settings: AppSettings,
    contentPadding: androidx.compose.foundation.layout.PaddingValues,
    modifier: Modifier,
    repository: ConfigRepository?,
) {
    var locations by remember { mutableStateOf<List<VpeenLocation>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var selectedRegion by remember(repository) {
        mutableStateOf((repository?.profiles?.value?.firstOrNull { it.isVpeen }?.source as? ProfileSource.Vpeen)?.region.orEmpty())
    }
    val scope = rememberCoroutineScope()
    fun refresh() {
        if (loading) return
        loading = true
        error = ""
        scope.launch {
            runCatching { withContext(Dispatchers.IO) { dev.spider.spider.platform.loadVpeenLocations() } }
                .onSuccess { locations = it; if (it.isEmpty()) error = "No free locations were returned." }
                .onFailure { error = it.message ?: "Could not load VeePN locations." }
            loading = false
        }
    }
    LaunchedEffect(Unit) { refresh() }
    val profile = repository?.profiles?.value?.firstOrNull { it.isVpeen }
    SettingsPageScaffold(
        title = stringResource(Res.string.settings_vpeen),
        subtitle = stringResource(Res.string.settings_vpeen_summary),
        contentPadding = contentPadding,
        modifier = modifier,
        art = SettingsArts.Vpeen,
    ) {
        SettingsGroup {
            SettingsInfoRow(stringResource(Res.string.settings_vpeen), stringResource(Res.string.settings_vpeen_about))
        }
        SettingsGroup(stringResource(Res.string.settings_location)) {
            Box {
                TextButton(onClick = { expanded = true }, enabled = !loading) {
                    Text(if (selectedRegion.isBlank()) stringResource(Res.string.settings_location_optimal) else locations.firstOrNull { it.region == selectedRegion }?.let { "${it.name} · ${it.countryCode}" } ?: selectedRegion)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.settings_location_optimal)) },
                        onClick = { selectedRegion = ""; expanded = false; profile?.let { p -> repository?.update(p.copy(source = ProfileSource.Vpeen(""))) } },
                    )
                    locations.forEach { location ->
                        DropdownMenuItem(
                            text = { Text("${location.name} · ${location.countryCode}") },
                            onClick = {
                                selectedRegion = location.region
                                expanded = false
                                profile?.let { p -> repository?.update(p.copy(source = ProfileSource.Vpeen(location.region), address = location.region)) }
                            },
                        )
                    }
                }
            }
            if (loading) CircularProgressIndicator(Modifier.padding(12.dp))
            if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = ::refresh, enabled = !loading) { Text(stringResource(Res.string.settings_refresh_locations)) }
        }
        if (repository == null) Text(stringResource(Res.string.settings_provider_profile_missing))
    }
}

@Composable
internal fun FirefoxVpnSettingsPage(
    contentPadding: androidx.compose.foundation.layout.PaddingValues,
    modifier: Modifier,
    repository: ConfigRepository?,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var accounts by remember { mutableStateOf<List<kotlinx.serialization.json.JsonObject>>(emptyList()) }
    var locations by remember { mutableStateOf<List<kotlinx.serialization.json.JsonObject>>(emptyList()) }
    var selectedLocation by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var signInId by remember { mutableStateOf("") }
    var signInStep by remember { mutableStateOf("") }
    var challenge by remember { mutableStateOf("") }
    var challengeMessage by remember { mutableStateOf("") }
    var captchaData by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val actions = remember { Channel<FirefoxVpnAction>(Channel.UNLIMITED) }
    val json = remember { kotlinx.serialization.json.Json { ignoreUnknownKeys = true } }

    DisposableEffect(actions) {
        onDispose { actions.close() }
    }

    suspend fun reload() {
        val errors = mutableListOf<String>()
        runCatching {
            val accountDoc = json.parseToJsonElement(dev.spider.spider.platform.firefoxVpnRequest("GET", "accounts"))
            accounts = (accountDoc as? kotlinx.serialization.json.JsonObject)?.get("accounts")
                ?.jsonArray?.mapNotNull { it as? kotlinx.serialization.json.JsonObject }.orEmpty()
        }.onFailure { it.rethrowIfCancelled(); errors += it.message ?: "Could not load Firefox accounts." }
        runCatching {
            val locationDoc = json.parseToJsonElement(dev.spider.spider.platform.firefoxVpnRequest("GET", "locations"))
            locations = (locationDoc as? kotlinx.serialization.json.JsonObject)?.get("locations")
                ?.jsonArray?.mapNotNull { it as? kotlinx.serialization.json.JsonObject }.orEmpty()
        }.onFailure { it.rethrowIfCancelled(); errors += it.message ?: "Could not load Firefox VPN locations." }
        runCatching {
            val settingsDoc = json.parseToJsonElement(dev.spider.spider.platform.firefoxVpnRequest("GET", "settings")) as? kotlinx.serialization.json.JsonObject
            selectedLocation = (((settingsDoc?.get("config") as? kotlinx.serialization.json.JsonObject)
                ?.get("proxy") as? kotlinx.serialization.json.JsonObject)?.get("country") as? kotlinx.serialization.json.JsonPrimitive)
                ?.contentOrNull.orEmpty()
        }.onFailure { it.rethrowIfCancelled(); errors += it.message ?: "Could not load Firefox VPN settings." }
        error = errors.joinToString("\n")
    }

    LaunchedEffect(Unit) {
        busy = true
        reload()
        busy = false
    }
    LaunchedEffect(actions) {
        for (action in actions) {
            busy = true
            when (action) {
                is FirefoxVpnAction.SignIn -> {
                    error = ""
                    runCatching {
                        val payload = kotlinx.serialization.json.buildJsonObject {
                            put("email", action.email)
                            put("password", action.password)
                        }.toString()
                        val state = json.parseToJsonElement(
                            dev.spider.spider.platform.firefoxVpnRequest("POST", "signin", payload),
                        ) as kotlinx.serialization.json.JsonObject
                        signInId = state["id"]?.jsonPrimitive?.contentOrNull.orEmpty()
                        signInStep = state["step"]?.jsonPrimitive?.contentOrNull.orEmpty()
                        challengeMessage = state["message"]?.jsonPrimitive?.contentOrNull.orEmpty()
                        captchaData = state["captcha"]?.jsonPrimitive?.contentOrNull.orEmpty()
                    }.onFailure { it.rethrowIfCancelled(); error = it.message ?: "Could not start sign-in." }
                }
                is FirefoxVpnAction.Verify -> {
                    runCatching {
                        val payload = kotlinx.serialization.json.buildJsonObject { put("value", action.value) }.toString()
                        dev.spider.spider.platform.firefoxVpnRequest("POST", "signin/${action.signInId}", payload)
                        challenge = ""
                    }.onFailure { it.rethrowIfCancelled(); error = it.message ?: "The verification answer was rejected." }
                }
                is FirefoxVpnAction.SetLocation -> {
                    runCatching {
                        val payload = kotlinx.serialization.json.buildJsonObject { put("country", action.country) }.toString()
                        dev.spider.spider.platform.firefoxVpnRequest("POST", "location", payload)
                    }.onFailure { it.rethrowIfCancelled(); error = it.message.orEmpty() }
                }
                FirefoxVpnAction.Refresh -> {
                    error = ""
                    reload()
                }
            }
            busy = false
        }
    }
    LaunchedEffect(signInId) {
        if (signInId.isBlank()) return@LaunchedEffect
        while (true) {
            delay(900)
            runCatching {
                json.parseToJsonElement(dev.spider.spider.platform.firefoxVpnRequest("GET", "signin/$signInId")) as kotlinx.serialization.json.JsonObject
            }.onSuccess { state ->
                signInStep = state["step"]?.jsonPrimitive?.contentOrNull.orEmpty()
                challengeMessage = state["message"]?.jsonPrimitive?.contentOrNull.orEmpty()
                captchaData = state["captcha"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val signInError = state["error"]?.jsonPrimitive?.contentOrNull.orEmpty()
                if (signInError.isNotBlank()) error = signInError
                if (signInStep == "done") {
                    signInId = ""
                    password = ""
                    challenge = ""
                    reload()
                } else if (signInStep == "failed" || signInStep == "cancelled") {
                    signInId = ""
                }
            }.onFailure { it.rethrowIfCancelled(); error = it.message ?: "Could not check Firefox Account sign-in." }
            if (signInId.isBlank()) break
        }
    }

    SettingsPageScaffold(
        title = stringResource(Res.string.settings_firefox_vpn),
        subtitle = stringResource(Res.string.settings_firefox_vpn_summary),
        contentPadding = contentPadding,
        modifier = modifier,
        art = SettingsArts.FirefoxVpn,
    ) {
        SettingsGroup {
            SettingsInfoRow(stringResource(Res.string.settings_firefox_vpn), stringResource(Res.string.settings_firefox_vpn_about))
        }
        SettingsGroup(stringResource(Res.string.settings_firefox_login)) {
            if (accounts.isNotEmpty()) {
                accounts.forEach { account ->
                    val label = account["email"]?.jsonPrimitive?.contentOrNull.orEmpty()
                    val state = account["status"]?.jsonPrimitive?.contentOrNull.orEmpty()
                    Text("$label · $state", modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                }
            }
            OutlinedTextField(
                value = email, onValueChange = { email = it; error = "" }, modifier = Modifier.fillMaxWidth(),
                singleLine = true, label = { Text(stringResource(Res.string.settings_firefox_email)) },
            )
            OutlinedTextField(
                value = password, onValueChange = { password = it; error = "" }, modifier = Modifier.fillMaxWidth(),
                singleLine = true, label = { Text(stringResource(Res.string.settings_firefox_password)) },
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
            )
            Button(
                enabled = !busy && signInId.isBlank() && email.isNotBlank() && password.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (actions.trySend(FirefoxVpnAction.SignIn(email.trim(), password)).isSuccess) busy = true
                },
            ) { Text(stringResource(Res.string.settings_firefox_sign_in)) }
            if (signInId.isNotBlank()) {
                Text(challengeMessage.ifBlank { signInStep }, modifier = Modifier.padding(horizontal = 16.dp))
                if (signInStep == "need_captcha" && captchaData.isNotBlank()) {
                    dev.spider.spider.platform.decodeFirefoxVpnCaptcha(captchaData)?.let { image ->
                        Image(
                            bitmap = image,
                            contentDescription = challengeMessage,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        )
                    }
                }
                if (signInStep in setOf("need_email_code", "need_totp", "need_unblock_code", "need_captcha")) {
                    OutlinedTextField(
                        value = challenge, onValueChange = { challenge = it }, modifier = Modifier.fillMaxWidth(),
                        singleLine = true, label = { Text(stringResource(Res.string.settings_firefox_verification_code)) },
                    )
                    Button(
                        enabled = !busy && challenge.isNotBlank(), modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            if (actions.trySend(FirefoxVpnAction.Verify(signInId, challenge)).isSuccess) busy = true
                        },
                    ) { Text(stringResource(Res.string.settings_firefox_continue)) }
                }
            }
            if (error.isNotBlank()) Text(error, modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        SettingsGroup {
            Text(stringResource(Res.string.settings_location), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            Box {
                TextButton(onClick = { expanded = true }, enabled = !busy) {
                    val selected = locations.firstOrNull { it["code"]?.jsonPrimitive?.contentOrNull == selectedLocation }
                    val label = selected?.let { it["name"]?.jsonPrimitive?.contentOrNull.orEmpty() + " · " + selectedLocation }
                        ?: if (selectedLocation.equals("fastest", true)) stringResource(Res.string.settings_firefox_fastest)
                        else stringResource(Res.string.settings_location_optimal)
                    Text("$label  ${if (locations.isNotEmpty()) "▾" else "⟳"}")
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(text = { Text(stringResource(Res.string.settings_location_optimal)) }, onClick = {
                        expanded = false
                        selectedLocation = ""
                        if (actions.trySend(FirefoxVpnAction.SetLocation("")).isSuccess) busy = true
                    })
                    DropdownMenuItem(text = { Text(stringResource(Res.string.settings_firefox_fastest)) }, onClick = {
                        expanded = false
                        selectedLocation = "fastest"
                        if (actions.trySend(FirefoxVpnAction.SetLocation("fastest")).isSuccess) busy = true
                    })
                    locations.forEach { location ->
                        val code = location["code"]?.jsonPrimitive?.contentOrNull.orEmpty()
                        val name = location["name"]?.jsonPrimitive?.contentOrNull.orEmpty()
                        val city = location["city"]?.jsonPrimitive?.contentOrNull.orEmpty()
                        DropdownMenuItem(text = { Text(listOf(name, city, code).filter { it.isNotBlank() }.joinToString(" · ")) }, onClick = {
                            expanded = false; selectedLocation = code
                            if (actions.trySend(FirefoxVpnAction.SetLocation(code)).isSuccess) busy = true
                        })
                    }
                }
            }
            if (locations.isEmpty() && error.isBlank()) {
                Text(stringResource(Res.string.settings_refresh_locations), modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), style = MaterialTheme.typography.bodySmall)
            }
            if (error.isNotBlank()) Text(error, modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { if (actions.trySend(FirefoxVpnAction.Refresh).isSuccess) busy = true }, enabled = !busy) {
                Text(stringResource(Res.string.settings_refresh_locations))
            }
        }
    }
}
