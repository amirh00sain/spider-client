@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.spider.spider.ui.servers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.spider.spider.domain.config.MhrProfile
import dev.spider.spider.domain.config.SoarProfile
import dev.spider.spider.domain.config.ProviderProfileImport
import dev.spider.spider.domain.config.ImportedProviderProfile
import dev.spider.spider.ui.platform.appVerticalScroll

@Composable
fun MhrSheet(onDismiss: () -> Unit, onSave: (String, MhrProfile) -> Unit) {
    var name by remember { mutableStateOf("MHR") }
    var ids by remember { mutableStateOf("") }
    var auth by remember { mutableStateOf("") }
    val settings = MhrProfile(ids.lineSequence().map { it.trim() }.filter(String::isNotEmpty).toList(), auth.trim())
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().appVerticalScroll(rememberScrollState()).imePadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Add MHR", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Name") })
            OutlinedTextField(
                ids, { ids = it }, Modifier.fillMaxWidth().heightIn(min = 110.dp), minLines = 3,
                label = { Text("Deployment IDs · one per line") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            )
            OutlinedTextField(auth, { auth = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Auth key") })
            settings.validationError()?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = { onSave(name.trim().ifBlank { "MHR" }, settings) },
                enabled = settings.validationError() == null,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            ) { Text("Add MHR") }
        }
    }
}

@Composable
fun SoarSheet(onDismiss: () -> Unit, onSave: (String, SoarProfile) -> Unit) {
    var pasted by remember { mutableStateOf("") }
    val imported = remember(pasted) { ProviderProfileImport.parse(pasted) as? ImportedProviderProfile.Soar }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().appVerticalScroll(rememberScrollState()).imePadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Add Soar", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                pasted, { pasted = it }, Modifier.fillMaxWidth(), singleLine = true,
                label = { Text("Paste Soar profile link") },
                placeholder = { Text("soar://…") },
            )
            if (pasted.isNotBlank() && imported == null) {
                Text("Invalid Soar profile link or unsupported schema", color = MaterialTheme.colorScheme.error)
            }
            Button(
                onClick = { imported?.let { onSave(it.name, it.settings) } },
                enabled = imported != null,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            ) { Text("Add Soar") }
        }
    }
}
