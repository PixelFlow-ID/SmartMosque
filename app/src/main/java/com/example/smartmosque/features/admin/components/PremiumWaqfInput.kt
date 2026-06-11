package com.example.smartmosque.features.admin.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartmosque.ui.theme.BgPremium
import com.example.smartmosque.ui.theme.EmeraldDeep
import com.example.smartmosque.ui.theme.GrayInactive
import com.example.smartmosque.ui.theme.White

@Composable
fun PremiumWaqfInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    icon: ImageVector,
    isMultiLine: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 12.sp) },
        placeholder = { Text(placeholder, fontSize = 12.sp, color = GrayInactive) },
        leadingIcon = { Icon(icon, null, tint = EmeraldDeep) },
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isMultiLine) 120.dp else 60.dp),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = EmeraldDeep,
            unfocusedBorderColor = GrayInactive,
            focusedLabelColor = EmeraldDeep,
            cursorColor = EmeraldDeep,
            focusedContainerColor = White,
            unfocusedContainerColor = BgPremium.copy(alpha = 0.3f)
        ),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        maxLines = if (isMultiLine) 5 else 1
    )
}