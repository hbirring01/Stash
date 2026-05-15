package com.example.creditcardapp.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.creditcardapp.domain.model.CreditCard
import com.example.creditcardapp.ui.format.expiry
import com.example.creditcardapp.ui.format.maskedNumber

private data class CardPalette(val from: Color, val to: Color, val onCard: Color = Color(0xFFF7F7F2))

private fun paletteFor(brand: String): CardPalette {
    return when (brand.trim().lowercase()) {
        "visa"        -> CardPalette(Color(0xFF1A2A6C), Color(0xFF2E5BCE))
        "mastercard", "mc" -> CardPalette(Color(0xFF3A1C1C), Color(0xFFB23A48))
        "amex", "american express" -> CardPalette(Color(0xFF0F3D3E), Color(0xFF2E7D6B))
        "discover"    -> CardPalette(Color(0xFF4A2C00), Color(0xFFE07A1F))
        else          -> CardPalette(Color(0xFF1B1D22), Color(0xFF3A3F47))
    }
}

@Composable
fun CreditCardTile(
    card: CreditCard,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    logoBase64: String? = null
) {
    val palette = paletteFor(card.brand)
    val shape = RoundedCornerShape(22.dp)
    val logoBitmap = remember(logoBase64) {
        logoBase64?.takeIf { it.isNotBlank() }?.let { decodeBase64Image(it) }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(if (compact) 2.2f else 1.6f)
            .clip(shape)
            .background(Brush.linearGradient(listOf(palette.from, palette.to)))
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                if (logoBitmap != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(width = 56.dp, height = 24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(palette.onCard.copy(alpha = 0.92f))
                            .padding(3.dp)
                    ) {
                        Image(
                            bitmap = logoBitmap,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    Text(
                        text = card.brand.uppercase(),
                        color = palette.onCard.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.align(Alignment.CenterStart)
                    )
                }
                ChipMark(
                    onCard = palette.onCard,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }

            Text(
                text = maskedNumber(card.last4),
                color = palette.onCard,
                fontWeight = FontWeight.Medium,
                fontSize = if (compact) 16.sp else 20.sp,
                letterSpacing = 2.sp
            )

            Box(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.align(Alignment.CenterStart)) {
                    Text(
                        text = "CARDHOLDER",
                        color = palette.onCard.copy(alpha = 0.55f),
                        style = MaterialTheme.typography.labelMedium,
                        fontSize = 9.sp
                    )
                    Text(
                        text = card.cardholderName.uppercase(),
                        color = palette.onCard,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Column(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "EXPIRES",
                        color = palette.onCard.copy(alpha = 0.55f),
                        style = MaterialTheme.typography.labelMedium,
                        fontSize = 9.sp
                    )
                    Text(
                        text = expiry(card.expiryMonth, card.expiryYear),
                        color = palette.onCard,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

private fun decodeBase64Image(base64: String): androidx.compose.ui.graphics.ImageBitmap? = runCatching {
    val cleaned = base64.substringAfter(",")
    val bytes = Base64.decode(cleaned, Base64.DEFAULT)
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
}.getOrNull()

@Composable
private fun ChipMark(onCard: Color, modifier: Modifier = Modifier) {
    val chipShape = RoundedCornerShape(6.dp)
    Box(
        modifier = modifier
            .size(width = 36.dp, height = 26.dp)
            .clip(chipShape)
            .background(onCard.copy(alpha = 0.18f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(5.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            repeat(3) {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(onCard.copy(alpha = 0.35f))
                )
            }
        }
        Spacer(
            modifier = Modifier
                .align(Alignment.Center)
                .width(2.dp)
                .fillMaxSize()
                .background(onCard.copy(alpha = 0.25f))
        )
    }
}
