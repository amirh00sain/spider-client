package dev.spider.spider.ui.servers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.spider.spider.domain.config.AetherProfile
import dev.spider.spider.shared.resources.Res
import dev.spider.spider.shared.resources.*
import dev.spider.spider.ui.platform.rememberAppSheetState
import org.jetbrains.compose.resources.stringResource

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AetherEditorSheet(
    initialName: String = "",
    initial: AetherProfile = AetherProfile(),
    onDismiss: () -> Unit,
    onSave: (String, AetherProfile) -> Unit,
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    var settings by remember(initial) { mutableStateOf(initial) }
    val error = settings.validationError()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberAppSheetState()) {
        Column(
            Modifier.fillMaxWidth().imePadding().heightIn(max = 720.dp)
                .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(Res.string.aether_editor_title), style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(name, { name = it }, label = { Text(stringResource(Res.string.manual_remark)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Text(stringResource(Res.string.aether_protocol), style = MaterialTheme.typography.labelLarge)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("wg", "masque", "gool", "mim", "wg-over-masque").forEach { option ->
                    FilterChip(settings.protocol == option, { settings = settings.copy(protocol = option) }, label = { Text(option.uppercase()) })
                }
            }
            if (settings.protocol in setOf("masque", "mim", "wg-over-masque")) {
                Text(stringResource(Res.string.aether_transport), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("h3", "h2").forEach { option ->
                        FilterChip(settings.transport == option, { settings = settings.copy(transport = option) }, label = { Text(option.uppercase()) })
                    }
                }
                AetherField(stringResource(Res.string.aether_masque_sni), settings.masqueSni) { settings = settings.copy(masqueSni = it) }
            }
            if (settings.protocol in setOf("gool", "mim", "wg-over-masque")) {
                AetherField(stringResource(Res.string.aether_outer_endpoint), settings.outerPeer, "host:port") { settings = settings.copy(outerPeer = it) }
                AetherField(stringResource(Res.string.aether_inner_endpoint), settings.innerPeer, "host:port") { settings = settings.copy(innerPeer = it) }
            } else {
                AetherField(stringResource(Res.string.aether_endpoint), settings.peer, "host:port · blank scans automatically") { settings = settings.copy(peer = it) }
            }
            AetherChoiceRow(stringResource(Res.string.aether_scan), settings.scan, AetherProfile.SCANS.toList()) { settings = settings.copy(scan = it) }
            AetherChoiceRow(stringResource(Res.string.aether_obfuscation), settings.obfuscation, AetherProfile.OBFUSCATIONS.toList()) { settings = settings.copy(obfuscation = it) }
            AetherChoiceRow(stringResource(Res.string.aether_ip_version), settings.ipVersion, AetherProfile.IP_VERSIONS.toList()) { settings = settings.copy(ipVersion = it) }
            AetherField(stringResource(Res.string.aether_dns), settings.dns) { settings = settings.copy(dns = it) }
            AetherField(stringResource(Res.string.aether_exit_location), settings.exitLocation) { settings = settings.copy(exitLocation = it) }
            if (settings.protocol in setOf("masque", "mim", "wg-over-masque")) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(settings.ech, { settings = settings.copy(ech = !settings.ech) }, label = { Text(stringResource(Res.string.aether_ech)) })
                    if (settings.transport == "h2") FilterChip(settings.fragment, { settings = settings.copy(fragment = !settings.fragment) }, label = { Text(stringResource(Res.string.aether_fragment)) })
                }
            }
            if (error != null) Text(stringResource(Res.string.aether_invalid_settings, error), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Button(
                onClick = { onSave(name.trim().ifEmpty { "Aether" }, settings) },
                enabled = error == null,
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            ) { Text(stringResource(Res.string.action_save)) }
        }
    }
}

@Composable
private fun AetherChoiceRow(label: String, value: String, options: List<String>, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { option ->
                FilterChip(value == option, { onSelect(option) }, label = { Text(option.uppercase()) })
            }
        }
    }
}

@Composable
private fun AetherField(label: String, value: String, placeholder: String? = null, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { placeholder?.let { Text(it) } },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}
