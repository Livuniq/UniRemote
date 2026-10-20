package com.example.universalremote.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TactileButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    text: String? = null,
    subText: String? = null,
    size: Dp = 56.dp,
    shape: Shape = CircleShape,
    primaryColor: Color = Color(0xFF2A2D34),
    highlightColor: Color = Color(0xFF383C45),
    contentColor: Color = Color.White,
    glowColor: Color? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.92f else 1.0f, label = "button_scale")

    val bgBrush = if (glowColor != null && isPressed) {
        Brush.radialGradient(
            colors = listOf(glowColor.copy(alpha = 0.8f), primaryColor)
        )
    } else {
        Brush.verticalGradient(
            colors = if (isPressed) {
                listOf(primaryColor, highlightColor)
            } else {
                listOf(highlightColor, primaryColor)
            }
        )
    }

    Box(
        modifier = modifier
            .scale(scale)
            .size(size)
            .shadow(
                elevation = if (isPressed) 1.dp else 5.dp,
                shape = shape,
                clip = false
            )
            .clip(shape)
            .background(bgBrush)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = text ?: "Remote Button",
                tint = contentColor,
                modifier = Modifier.size(size * 0.45f)
            )
        } else if (text != null) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = text,
                    color = contentColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
