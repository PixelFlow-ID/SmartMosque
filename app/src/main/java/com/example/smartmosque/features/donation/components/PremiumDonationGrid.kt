package com.example.smartmosque.features.donation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Chair
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartmosque.features.home.InfaqCategoryHome
import com.example.smartmosque.ui.theme.TextBlack
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun PremiumDonationGrid(onItemClick: (InfaqCategoryHome) -> Unit) {
    val menuList = listOf(
        InfaqCategoryHome("ops", "Operasional", Icons.Default.Mosque, Color(0xFFE0F2F1), Color(0xFF00695C)),
        InfaqCategoryHome("snack", "Jumat Berkah", Icons.Default.Restaurant, Color(0xFFFFF8E1), Color(0xFFFF8F00)),
        InfaqCategoryHome("alat", "Sarana", Icons.Default.Chair, Color(0xFFE3F2FD), Color(0xFF1565C0)),
        InfaqCategoryHome("kitab", "Wakaf Kitab", Icons.Default.Book, Color(0xFFF3E5F5), Color(0xFF7B1FA2))
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        menuList.forEach { item ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(80.dp).clickable { onItemClick(item) }
            ) {
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = RoundedCornerShape(22.dp),
                    color = item.color
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(item.icon, item.title, tint = item.iconColor, modifier = Modifier.size(28.dp))
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(item.title, fontSize = 12.sp, color = TextBlack, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            }
        }
    }
}