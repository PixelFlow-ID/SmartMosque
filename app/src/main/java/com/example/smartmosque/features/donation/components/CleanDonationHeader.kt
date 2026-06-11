package com.example.smartmosque.features.donation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.example.smartmosque.ui.theme.EmeraldDeep
import com.example.smartmosque.ui.theme.TextColorPrimary
import com.example.smartmosque.ui.theme.TextColorSecondary

@Composable
fun CleanDonationHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.VolunteerActivism,
            contentDescription = null,
            tint = EmeraldDeep.copy(alpha = 0.05f),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(110.dp)
                .offset(x = 20.dp, y = 10.dp)
                .rotate(-10f)
        )

        Column {
            Text(
                text = "INVESTASI AKHIRAT",
                fontSize = 11.sp,
                color = EmeraldDeep,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Infaq & Wakaf",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextColorPrimary,
                letterSpacing = (-0.5).sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Salurkan harta terbaikmu untuk\nkebaikan yang mengalir abadi.",
                fontSize = 14.sp,
                color = TextColorSecondary,
                lineHeight = 20.sp
            )
        }
    }
}