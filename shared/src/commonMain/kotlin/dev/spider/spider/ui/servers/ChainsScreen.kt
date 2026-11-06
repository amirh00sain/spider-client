@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.spider.spider.ui.servers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.spider.spider.data.config.ConfigRepository
import dev.spider.spider.domain.config.VpnProfile
import dev.spider.spider.shared.resources.Res
import dev.spider.spider.shared.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** A single entry point for the two chain types the Spider runtime can execute. */
@Composable
fun ChainsScreen(
    repository: ConfigRepository,
    contentPadding: PaddingValues,
    onChainActivated: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val profiles by repository.profiles.collectAsStateWithLifecycle()
    val activeId by repository.activeId.collectAsStateWithLifecycle()
    val chains = profiles.filter { it.isProxyChain || it.isCrossChain }
    val chainCandidates = profiles.filter {
        !it.isLocked && it.rawPayload() != null && (!it.isCustom || it.isServerless) &&
            !it.isManagedTunnel && !it.isProxyChain && !it.isCrossChain && !it.isSingBoxConfig
    }
    val crossCarriers = profiles.filter { !it.isLocked && it.canCarryChain && !it.isCrossChain }
    val crossExits = profiles.filter { !it.isLocked && it.canDialThroughProxy && !it.isCrossChain }

    var proxyEditor by remember { mutableStateOf<VpnProfile?>(null) }
    var crossEditor by remember { mutableStateOf<VpnProfile?>(null) }
    var pendingDelete by remember { mutableStateOf<VpnProfile?>(null) }

    Column(modifier.fillMaxSize().padding(contentPadding)) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(Res.string.chains_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(stringResource(Res.string.chains_intro), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { proxyEditor = VpnProfile.chainDraft() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                ) { Text(stringResource(Res.string.chains_new_proxy), maxLines = 1, overflow = TextOverflow.Ellipsis) }
                OutlinedButton(
                    onClick = { crossEditor = VpnProfile.crossChainDraft() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                ) { Text(stringResource(Res.string.chains_new_tunnel), maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
        }

        if (chains.isEmpty()) {
            Column(
                Modifier.fillMaxWidth().weight(1f).padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(Res.string.chains_empty), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(Res.string.chains_empty_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(chains, key = { it.id }) { chain ->
                    val isActive = chain.id == activeId
                    ChainCard(
                        profile = chain,
                        profiles = profiles,
                        active = isActive,
                        onActivate = {
                            repository.setActive(chain.id)
                            onChainActivated()
                        },
                        onEdit = {
                            if (chain.isProxyChain) proxyEditor = chain else crossEditor = chain
                        },
                        onDelete = { pendingDelete = chain },
                    )
                }
            }
        }
    }

    proxyEditor?.let { target ->
        val initial = if (target.isProxyChain) target.proxyChainSettings().orEmpty() else emptyList()
        ProxyChainSheet(
            candidates = chainCandidates,
            initialName = target.name.takeUnless { target.isProxyChain && target.id == CHAIN_DRAFT_ID }.orEmpty(),
            initialMemberIds = initial,
            onDismiss = { proxyEditor = null },
            onSave = { name, memberIds ->
                repository.addProxyChain(memberIds, name, id = target.id.takeUnless { it == CHAIN_DRAFT_ID })
                proxyEditor = null
            },
        )
    }

    crossEditor?.let { target ->
        val members = target.crossChainSettings()
        CrossChainSheet(
            exits = crossExits,
            carriers = crossCarriers,
            pairError = { inner, outer -> repository.udpMismatch(inner, outer)?.message },
            initialName = target.name.takeUnless { target.id == CHAIN_DRAFT_ID }.orEmpty(),
            initialInnerId = members?.first.orEmpty(),
            initialOuterId = members?.second.orEmpty(),
            onDismiss = { crossEditor = null },
            onSave = { name, innerId, outerId ->
                repository.addCrossChain(innerId, outerId, name, id = target.id.takeUnless { it == CHAIN_DRAFT_ID })
                crossEditor = null
            },
        )
    }

    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(Res.string.action_delete)) },
            text = { Text(target.name) },
            confirmButton = {
                TextButton(onClick = {
                    repository.remove(target.id)
                    pendingDelete = null
                }) { Text(stringResource(Res.string.action_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text(stringResource(Res.string.action_cancel)) } },
        )
    }
}

private const val CHAIN_DRAFT_ID = "__new_chain__"

private fun VpnProfile.Companion.chainDraft() = fromProxyChain(
    memberIds = emptyList(), id = CHAIN_DRAFT_ID, addedAt = 0L, name = "",
)

private fun VpnProfile.Companion.crossChainDraft() = fromCrossChain(
    innerId = "", outerId = "", id = CHAIN_DRAFT_ID, addedAt = 0L, name = "",
    innerName = "", outerName = "",
)

@Composable
private fun ChainCard(
    profile: VpnProfile,
    profiles: List<VpnProfile>,
    active: Boolean,
    onActivate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val byId = remember(profiles) { profiles.associateBy { it.id } }
    val members = if (profile.isProxyChain) {
        profile.proxyChainSettings().orEmpty().mapNotNull(byId::get)
    } else {
        profile.crossChainSettings()?.let { (inner, outer) -> listOfNotNull(byId[inner], byId[outer]) }.orEmpty()
    }
    Card(
        onClick = onActivate,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (active) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(profile.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        if (active) stringResource(Res.string.chains_active)
                        else stringResource(if (profile.isCrossChain) Res.string.chains_tunnel_kind else Res.string.chains_proxy_kind),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onEdit) { Icon(painterResource(Res.drawable.ic_edit), contentDescription = stringResource(Res.string.action_edit)) }
                IconButton(onClick = onDelete) { Icon(painterResource(Res.drawable.ic_delete), contentDescription = stringResource(Res.string.action_delete)) }
            }
            if (members.isNotEmpty()) {
                Text(
                    members.joinToString("  →  ") { it.name },
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                Text(
                    stringResource(Res.string.chain_member_missing, "—"),
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
