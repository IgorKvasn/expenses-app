package com.example.expensetracker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SuccessAnimationOverlay(
    visible: Boolean,
    label: String,
    onAnimationEnd: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(200)),
    ) {
        val coinRotation = remember { Animatable(0f) }
        val coinScale = remember { Animatable(1f) }
        val coinAlpha = remember { Animatable(1f) }
        var showCheckmark by remember { mutableStateOf(false) }
        val checkmarkScale = remember { Animatable(0f) }
        var showLabel by remember { mutableStateOf(false) }
        val labelAlpha = remember { Animatable(0f) }
        val labelOffsetY = remember { Animatable(24f) }

        LaunchedEffect(Unit) {
            // Phase 1: Coin spin (800ms)
            launch {
                coinRotation.animateTo(
                    targetValue = 720f,
                    animationSpec = tween(durationMillis = 800, easing = EaseInOut),
                )
            }
            launch {
                coinScale.animateTo(1.3f, animationSpec = tween(400, easing = EaseInOut))
                coinScale.animateTo(1f, animationSpec = tween(400, easing = EaseInOut))
            }
            delay(800)

            // Phase 2: Coin shrink (300ms)
            launch { coinScale.animateTo(0f, animationSpec = tween(300)) }
            coinAlpha.animateTo(0f, animationSpec = tween(300))

            // Phase 3: Checkmark pop (400ms) + label fade (300ms)
            showCheckmark = true
            showLabel = true
            launch {
                checkmarkScale.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 400,
                        easing = androidx.compose.animation.core.CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f),
                    ),
                )
            }
            launch {
                labelAlpha.animateTo(1f, animationSpec = tween(300))
            }
            launch {
                labelOffsetY.animateTo(0f, animationSpec = tween(300))
            }

            // Phase 4: Hold then navigate
            delay(500)
            onAnimationEnd()
        }

        // Block back-press during animation
        BackHandler {}

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f)),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.Center) {
                    // Coin
                    if (!showCheckmark) {
                        Text(
                            text = "🪙",
                            fontSize = 64.sp,
                            modifier = Modifier
                                .graphicsLayer {
                                    rotationY = coinRotation.value
                                    scaleX = coinScale.value
                                    scaleY = coinScale.value
                                    alpha = coinAlpha.value
                                },
                        )
                    }

                    // Checkmark
                    if (showCheckmark) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .scale(checkmarkScale.value)
                                .clip(CircleShape)
                                .background(Color(0xFF4CAF50)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "✓",
                                color = Color.White,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (showLabel) {
                    Text(
                        text = label,
                        color = Color(0xFF4CAF50),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.graphicsLayer {
                            alpha = labelAlpha.value
                            translationY = labelOffsetY.value
                        },
                    )
                }
            }
        }
    }
}
