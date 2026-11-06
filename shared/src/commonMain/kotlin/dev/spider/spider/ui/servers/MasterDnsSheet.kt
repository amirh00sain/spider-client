@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.spider.spider.ui.servers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.spider.spider.domain.config.MasterDnsProfile
import dev.spider.spider.domain.config.ProviderProfileImport
import dev.spider.spider.domain.config.ImportedProviderProfile
import dev.spider.spider.ui.platform.appVerticalScroll

@Composable
fun MasterDnsSheet(
    initial: MasterDnsProfile? = null,
    initialName: String = "",
    onDismiss: () -> Unit,
    onSave: (name: String, MasterDnsProfile) -> Unit,
) {
    var pasted by remember { mutableStateOf("") }
    val imported = remember(pasted) { ProviderProfileImport.parse(pasted) as? ImportedProviderProfile.MasterDns }

    ModalBottomSheet(
        onDismissRequest = onDismiss,

        sheetState = rememberBottomSheetState(SheetValue.Hidden, setOf(SheetValue.Hidden, SheetValue.Expanded)),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .appVerticalScroll(rememberScrollState())
                .imePadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Add MasterDNS", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                pasted, { pasted = it }, Modifier.fillMaxWidth(), singleLine = true,
                label = { Text("Paste MasterDNS profile link") }, placeholder = { Text("masterdns://…") },
            )
            if (pasted.isNotBlank() && imported == null) {
                Text("Invalid MasterDNS profile link or unsupported schema", color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = {
                    imported?.let { onSave(it.name, it.settings) }
                },
                enabled = imported != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Add MasterDNS", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
