package com.example.universalremote.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RockerSwitch(
    title: String,
    onUpClick: () -> Unit,
    onDownClick: () -> Unit,
    modifier: Modifier = Modifier,
    isArrowType: Boolean = false
) {
    val upSource = remember { MutableInteractionSource() }
    val downSource = remember { MutableInteractionSource() }
    val isUpPressed by upSource.collectIsPressedAsState()
    val isDownPressed by downSource.collectIsPressedAsState()

    val upScale by animateFloatAsState(targetValue = if (isUpPressed) 0.93f else 1.0f, label = "up_scale")
    val downScale by animateFloatAsState(targetValue = if (isDownPressed) 0.93f else 1.0f, label = "down_scale")

    val containerShape = RoundedCornerShape(28.dp)

    Box(
        modifier = modifier
            .width(66.dp)
            .height(148.dp)
            .shadow(6.dp, shape = containerShape)
            .clip(containerShape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF2E323B),
                        Color(0xFF1E2127)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Upper Button (+)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .scale(upScale)
                    .clickable(
                        interactionSource = upSource,
                        indication = null,
                        onClick = onUpClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isArrowType) Icons.Filled.KeyboardArrowUp else Icons.Filled.Add,
                    contentDescription = "$title Up",
                    tint = if (isUpPressed) Color(0xFF64B5F6) else Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Center Label
            Text(
                text = title,
                color = Color(0xFF90A4AE),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            // Lower Button (-)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .scale(downScale)
                    .clickable(
                        interactionSource = downSource,
                        indication = null,
                        onClick = onDownClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isArrowType) Icons.Filled.KeyboardArrowDown else Icons.Filled.Remove,
                    contentDescription = "$title Down",
                    tint = if (isDownPressed) Color(0xFF64B5F6) else Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
