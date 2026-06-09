package com.catedra.tpinativo.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import com.catedra.tpinativo.R
import com.catedra.tpinativo.ui.theme.HabitBackground
import com.catedra.tpinativo.ui.theme.HabitCoral
import com.catedra.tpinativo.ui.theme.HabitViolet
import kotlinx.coroutines.delay

private val FRASES = listOf(
    "Pequeños pasos, grandes cambios.",
    "Un día a la vez, un hábito a la vez.",
    "La constancia es la madre del éxito.",
    "Hoy es el mejor día para empezar.",
    "El progreso, no la perfección.",
    "Cada hábito construye tu futuro.",
    "Sé el cambio que querés ver en vos mismo.",
    "Los grandes logros nacen de hábitos simples.",
    "Confía en el proceso.",
    "Tu mejor versión te está esperando."
)

@Composable
fun SplashScreen(onSplashTerminado: () -> Unit) {

    val frase = remember { FRASES.random() }
    val logoScale = remember { Animatable(0.6f) }
    val logoAlpha = remember { Animatable(0f) }
    val textoAlpha = remember { Animatable(0f) }
    val fraseAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        logoAlpha.animateTo(1f, animationSpec = tween(500))
        logoScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
        delay(150)
        textoAlpha.animateTo(1f, animationSpec = tween(400))
        delay(200)
        fraseAlpha.animateTo(1f, animationSpec = tween(500))

        delay(1800)
        onSplashTerminado()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HabitBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {

            Image(
                painter = painterResource(id = R.drawable.logo_habitflow),
                contentDescription = "HabitFlow logo",
                modifier = Modifier
                    .size(150.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value)
            )

            Spacer(modifier = Modifier.height(20.dp))

            GradientText(
                text = "HabitFlow",
                modifier = Modifier.alpha(textoAlpha.value)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = frase,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(fraseAlpha.value)
            )
        }
    }
}

@Composable
private fun GradientText(text: String, modifier: Modifier = Modifier) {
    val gradient = Brush.horizontalGradient(
        colors = listOf(HabitViolet, HabitCoral)
    )
    Text(
        text = text,
        style = MaterialTheme.typography.headlineLarge.copy(
            brush = gradient,
            fontSize = 42.sp
        ),
        modifier = modifier
    )
}
