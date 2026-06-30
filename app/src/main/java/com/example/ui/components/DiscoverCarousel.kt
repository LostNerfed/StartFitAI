package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentPrimary

data class DiscoverCardModel(
    val id: String,
    val title: String,
    val description: String,
    val tag: String,
    val type: DiscoverCardType
)

enum class DiscoverCardType {
    CHALLENGE, TIP, PR, NEW_USER, ROUTINE, FITNESS_TIP
}

@Composable
fun DiscoverCarousel(
    cards: List<DiscoverCardModel>,
    onCardClick: (DiscoverCardModel) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Para Ti",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(cards.size) { index ->
                val card = cards[index]
                DiscoverCard(card = card, cardIndex = index, onClick = { onCardClick(card) })
            }
        }
    }
}

@Composable
fun DiscoverCard(
    card: DiscoverCardModel,
    cardIndex: Int,
    onClick: () -> Unit
) {
    val (backgroundBrush, tagBackground, icon) = when (card.type) {
        DiscoverCardType.PR -> Triple(
            Brush.linearGradient(listOf(Color(0xFFB8860B), Color(0xFFDAA520))),
            Color.Black.copy(alpha = 0.5f),
            Icons.Default.Star
        )
        DiscoverCardType.CHALLENGE -> Triple(
            Brush.linearGradient(listOf(Color(0xFFE64A19), Color(0xFFFF7043))),
            Color.Black.copy(alpha = 0.5f),
            Icons.Default.FitnessCenter
        )
        DiscoverCardType.TIP -> Triple(
            Brush.linearGradient(listOf(Color(0xFF00796B), Color(0xFF26A69A))),
            Color.Black.copy(alpha = 0.5f),
            Icons.Default.Restaurant
        )
        DiscoverCardType.NEW_USER -> Triple(
            Brush.linearGradient(listOf(Color(0xFF303F9F), Color(0xFF5C6BC0))),
            Color.Black.copy(alpha = 0.5f),
            Icons.Default.ChevronRight
        )
        DiscoverCardType.ROUTINE -> Triple(
            Brush.linearGradient(listOf(Color(0xFF1E1E1E), Color(0xFF2C2C2C))),
            Color.Black.copy(alpha = 0.5f),
            Icons.Default.FitnessCenter
        )
        DiscoverCardType.FITNESS_TIP -> Triple(
            Brush.linearGradient(listOf(Color(0xFF303F9F), Color(0xFF5C6BC0))),
            Color.Black.copy(alpha = 0.5f),
            null
        )
    }

    val isPR = card.type == DiscoverCardType.PR

    Box(
        modifier = Modifier
            .width(260.dp)
            .height(160.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = if (isPR) 1.dp else 0.dp,
                color = if (isPR) Color(0xFFFFD700).copy(alpha = 0.5f) else Color.Transparent,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
    ) {
        val cycleIndex = remember { (System.currentTimeMillis() / (3 * 24 * 60 * 60 * 1000L)).toInt() }
        val availableImages = remember(card.type) {
            if (card.type == DiscoverCardType.TIP) {
                listOf(
                    com.example.R.drawable.food_1,
                    com.example.R.drawable.food_2,
                    com.example.R.drawable.food_3,
                    com.example.R.drawable.food_4,
                    com.example.R.drawable.food_5,
                    com.example.R.drawable.food_6,
                    com.example.R.drawable.food_7,
                    com.example.R.drawable.food_8
                )
            } else {
                listOf(
                    com.example.R.drawable.carousel_1,
                    com.example.R.drawable.carousel_2,
                    com.example.R.drawable.carousel_3,
                    com.example.R.drawable.carousel_4,
                    com.example.R.drawable.carousel_5,
                    com.example.R.drawable.carousel_6
                )
            }
        }
        val shuffledImages = remember(cycleIndex) {
            availableImages.shuffled(kotlin.random.Random(cycleIndex))
        }
        val imageId = shuffledImages[cardIndex % shuffledImages.size]

        Image(
            painter = painterResource(id = imageId),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Overlay: Gradiente inferior para que el texto resalte (estilo Nike Training / Fitbod)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)),
                        startY = 100f // Ajusta esto según qué tan alto quieres que llegue la sombra
                    )
                )
        )

        // Wrapper interior para mantener el padding original de 20.dp sin afectar las imágenes de fondo
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // Tag
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .background(tagBackground, RoundedCornerShape(20.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = card.tag.uppercase(),
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }

        // Icon Button (Only show if icon is not null)
        if (icon != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(36.dp)
                    .background(if (isPR) Color.Black.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.2f), CircleShape)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Content
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(end = if (icon != null) 46.dp else 0.dp)
        ) {
            Text(
                text = card.title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = card.description,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 12.sp,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
        }
    }
}
