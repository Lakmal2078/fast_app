package com.example.fastcash.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fastcash.data.model.SavedBankPreset
import com.example.fastcash.ui.theme.DarkSurface
import com.example.fastcash.ui.theme.NeonCyan
import com.example.fastcash.ui.theme.NeonPink
import com.example.fastcash.ui.theme.TextMuted
import com.example.fastcash.ui.theme.TextPrimary
import com.example.fastcash.ui.theme.TextSecondary

@Composable
fun SavedPlayerIdChipsRow(
    savedPlayerIds: List<String>,
    currentPlayerId: String,
    language: String,
    accentColor: Color,
    isSaving: Boolean,
    onSelect: (String) -> Unit,
    onSaveCurrent: () -> Unit,
    onRemove: (String) -> Unit
) {
    val trimmed = currentPlayerId.trim()
    val canSaveCurrent = trimmed.length >= 4 && trimmed !in savedPlayerIds

    if (savedPlayerIds.isEmpty() && !canSaveCurrent) return

    Text(
        text = if (language == "si") "සුරකින ලද PLAYER IDS (1-TAP)" else "SAVED PLAYER IDS (1-TAP)",
        style = MaterialTheme.typography.labelSmall,
        color = accentColor,
        fontSize = 10.sp
    )
    Spacer(modifier = Modifier.padding(top = 4.dp))
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("saved_player_ids_row"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(savedPlayerIds, key = { it }) { id ->
            val isSelected = trimmed == id
            Row(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) accentColor.copy(alpha = 0.18f) else DarkSurface)
                    .border(
                        1.dp,
                        if (isSelected) accentColor else Color(0x33FFFFFF),
                        RoundedCornerShape(6.dp)
                    )
                    .clickable { onSelect(id) }
                    .padding(start = 10.dp, end = 2.dp)
                    .testTag("player_id_chip_$id"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = if (isSelected) accentColor else TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = id,
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) accentColor else TextPrimary
                )
                IconButton(
                    onClick = { onRemove(id) },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("remove_player_id_chip_$id")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove saved Player ID $id",
                        tint = TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        if (canSaveCurrent) {
            item(key = "save_current_player_id") {
                Row(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(accentColor.copy(alpha = 0.12f))
                        .border(1.dp, accentColor.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                        .clickable(enabled = !isSaving) { onSaveCurrent() }
                        .padding(horizontal = 12.dp)
                        .testTag("save_player_id_chip"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (language == "si") "ID සුරකින්න" else "Save $trimmed",
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun SavedBankPresetsRow(
    savedPresets: List<SavedBankPreset>,
    currentBank: String,
    currentAccountNumber: String,
    language: String,
    isSaving: Boolean,
    onSelect: (SavedBankPreset) -> Unit,
    onSaveCurrent: () -> Unit,
    onRemove: (String) -> Unit
) {
    val cleanAcc = currentAccountNumber.trim()
    val alreadySaved = savedPresets.any {
        it.bank.equals(currentBank.trim(), ignoreCase = true) && it.accountNumber == cleanAcc
    }
    val canSaveCurrent = currentBank.isNotBlank() && cleanAcc.length >= 6 && !alreadySaved

    if (savedPresets.isEmpty() && !canSaveCurrent) return

    Text(
        text = if (language == "si") "සුරකින ලද බැංකු ගිණුම් (1-TAP)" else "SAVED BANK ACCOUNTS (1-TAP)",
        style = MaterialTheme.typography.labelSmall,
        color = NeonCyan,
        fontSize = 10.sp
    )
    Spacer(modifier = Modifier.padding(top = 4.dp))
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("saved_bank_presets_row"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(savedPresets, key = { it.id }) { preset ->
            val isSelected = preset.bank.equals(currentBank.trim(), ignoreCase = true) &&
                preset.accountNumber == cleanAcc
            val tail = if (preset.accountNumber.length > 4) {
                "••••${preset.accountNumber.takeLast(4)}"
            } else {
                preset.accountNumber
            }
            Row(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) NeonPink.copy(alpha = 0.18f) else DarkSurface)
                    .border(
                        1.dp,
                        if (isSelected) NeonPink else NeonCyan.copy(alpha = 0.35f),
                        RoundedCornerShape(6.dp)
                    )
                    .clickable { onSelect(preset) }
                    .padding(start = 10.dp, end = 2.dp)
                    .testTag("bank_preset_chip_${preset.id}"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = if (isSelected) NeonPink else NeonCyan,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${preset.bank.substringBefore(" (")} · $tail",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) NeonPink else TextPrimary
                )
                IconButton(
                    onClick = { onRemove(preset.id) },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("remove_bank_preset_chip_${preset.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove saved bank ${preset.bank}",
                        tint = TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        if (canSaveCurrent) {
            item(key = "save_current_bank") {
                Row(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(NeonCyan.copy(alpha = 0.12f))
                        .border(1.dp, NeonCyan.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                        .clickable(enabled = !isSaving) { onSaveCurrent() }
                        .padding(horizontal = 12.dp)
                        .testTag("save_bank_preset_chip"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (language == "si") "ගිණුම සුරකින්න" else "Save Bank Details",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
