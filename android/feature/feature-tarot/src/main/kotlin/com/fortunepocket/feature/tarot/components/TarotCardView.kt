package com.fortunepocket.feature.tarot.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fortunepocket.core.model.TarotCard
import com.fortunepocket.core.ui.theme.AppColors
import java.util.Locale

// MARK: - Size presets

enum class CardSize(val widthDp: Dp) {
    SMALL(78.dp),
    MEDIUM(100.dp),
    LARGE(130.dp);

    val heightDp: Dp get() = Dp(widthDp.value * 1.6f)
}

// MARK: - Public composable

/**
 * Renders a tarot card face-up (using the card's colour palette) or face-down.
 * Pass [card] = null or [faceDown] = true to show the decorative back.
 */
@Composable
fun TarotCardView(
    card:      TarotCard? = null,
    isUpright: Boolean    = true,
    faceDown:  Boolean    = false,
    size:      CardSize   = CardSize.MEDIUM,
    modifier:  Modifier   = Modifier
) {
    if (faceDown || card == null) {
        CardBack(size = size, modifier = modifier)
    } else {
        CardFront(card = card, isUpright = isUpright, size = size, modifier = modifier)
    }
}

// MARK: - Front face

@Composable
private fun CardFront(
    card:      TarotCard,
    isUpright: Boolean,
    size:      CardSize,
    modifier:  Modifier
) {
    val isZh    = Locale.getDefault().language == "zh"
    val primary = hexColor(card.colorPrimary)
    val accent  = hexColor(card.colorAccent)
    val w       = size.widthDp
    val h       = size.heightDp

    Box(
        modifier = modifier
            .size(width = w, height = h)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(primary, primary.copy(alpha = 0.70f))
                )
            )
            .border(
                width = 1.2.dp,
                color = AppColors.accentGold.copy(alpha = 0.55f),
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(horizontal = 4.dp)
        ) {
            // Symbol circle
            Box(
                modifier = Modifier
                    .size(Dp(w.value * 0.54f))
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text     = card.symbol,
                    fontSize = (w.value * 0.28f).sp,
                    color    = accent
                )
            }

            Spacer(Modifier.height(Dp(w.value * 0.07f)))

            // Card name
            Text(
                text       = if (isZh) card.nameZh else card.nameEn,
                fontSize   = (w.value * 0.095f).sp,
                fontWeight = FontWeight.Medium,
                color      = Color.White.copy(alpha = 0.9f),
                textAlign  = TextAlign.Center,
                maxLines   = 2
            )

            Spacer(Modifier.height(Dp(w.value * 0.04f)))

            // Upright / Reversed tag
            val tag = when {
                isUpright && isZh  -> "正位"
                isUpright && !isZh -> "Upright"
                !isUpright && isZh -> "逆位"
                else               -> "Reversed"
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text     = (if (isUpright) "↑ " else "↶ ") + tag,
                    fontSize = (w.value * 0.08f).sp,
                    fontWeight = FontWeight.SemiBold,
                    color    = accent.copy(alpha = 0.92f)
                )
            }
        }
    }
}

// MARK: - Back face

@Composable
private fun CardBack(size: CardSize, modifier: Modifier) {
    val w = size.widthDp
    val h = size.heightDp

    Box(
        modifier = modifier
            .size(width = w, height = h)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(AppColors.backgroundElevated, Color(0xFF2A1F50))
                )
            )
            .border(
                width = 1.2.dp,
                color = AppColors.accentGold.copy(alpha = 0.35f),
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        // Inner decorative border
        Box(
            modifier = Modifier
                .size(width = Dp(w.value - 12f), height = Dp(h.value - 12f))
                .border(
                    width = 0.5.dp,
                    color = AppColors.accentGold.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                )
        )
        Text(
            text     = "✦",
            fontSize = (w.value * 0.38f).sp,
            color    = AppColors.accentGold.copy(alpha = 0.35f)
        )
    }
}

// MARK: - Hex colour parser

private fun hexColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(
            if (hex.startsWith("#")) hex else "#$hex"
        ))
    } catch (_: Exception) {
        AppColors.backgroundElevated
    }
}
