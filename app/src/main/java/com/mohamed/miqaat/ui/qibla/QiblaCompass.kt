package com.mohamed.miqaat.ui.qibla

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.mohamed.miqaat.R
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Cadran de boussole : la rose des vents tourne avec l'appareil, la marque de
 * la Kaaba reste sur l'azimut de la Qibla. L'utilisateur tourne jusqu'à ce que
 * la marque rejoigne le repère fixe en haut.
 *
 * Tout est dessiné au Canvas avec les couleurs du thème → suit automatiquement
 * les modes clair et sombre.
 */
@Composable
fun QiblaCompass(
    qiblaBearing: Double,
    deviceHeading: Double?,
    isAligned: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val textMeasurer = rememberTextMeasurer()

    // Sans cap (pas encore de mesure, ou pas de magnétomètre), le nord du cadran
    // reste en haut : l'angle affiché est alors une valeur « carte », pas une visée.
    val heading = deviceHeading ?: 0.0
    val needleColor by animateColorAsState(
        targetValue = if (isAligned) colors.primary else colors.secondary,
        label = "qiblaNeedleColor",
    )
    val markerColor by animateColorAsState(
        targetValue = if (isAligned) colors.primary else colors.tertiary,
        label = "qiblaMarkerColor",
    )

    val cardinals = listOf(
        0.0 to stringResource(R.string.compass_north),
        90.0 to stringResource(R.string.compass_east),
        180.0 to stringResource(R.string.compass_south),
        270.0 to stringResource(R.string.compass_west),
    )
    val labelStyle = MaterialTheme.typography.labelLarge

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        // Marge pour le repère fixe qui déborde au-dessus du cadran.
        val radius = min(size.width, size.height) / 2f - 18.dp.toPx()

        drawDialFace(center, radius, colors.surfaceContainerLow, colors.outlineVariant)
        drawTicks(center, radius, heading, colors.outline, colors.onSurfaceVariant)
        drawCardinals(center, radius, heading, cardinals, textMeasurer, labelStyle, colors)
        drawQiblaNeedle(center, radius, qiblaBearing - heading, needleColor)
        drawKaabaMarker(center, radius, qiblaBearing - heading, markerColor, colors.onPrimary)
        drawFixedIndex(center, radius, colors.primary)

        drawCircle(color = colors.surface, radius = 6.dp.toPx(), center = center)
        drawCircle(
            color = needleColor,
            radius = 6.dp.toPx(),
            center = center,
            style = Stroke(width = 2.dp.toPx()),
        )
    }
}

private fun DrawScope.drawDialFace(
    center: Offset,
    radius: Float,
    face: Color,
    ring: Color,
) {
    drawCircle(color = face, radius = radius, center = center)
    drawCircle(
        color = ring,
        radius = radius,
        center = center,
        style = Stroke(width = 1.5.dp.toPx()),
    )
    drawCircle(
        color = ring.copy(alpha = 0.5f),
        radius = radius * 0.62f,
        center = center,
        style = Stroke(width = 1.dp.toPx()),
    )
}

/** Graduations tous les 15° ; les quatre points cardinaux sont plus marqués. */
private fun DrawScope.drawTicks(
    center: Offset,
    radius: Float,
    heading: Double,
    minor: Color,
    major: Color,
) {
    for (angle in 0 until 360 step 15) {
        val isCardinal = angle % 90 == 0
        val length = if (isCardinal) 14.dp.toPx() else 7.dp.toPx()
        rotate(degrees = (angle - heading).toFloat(), pivot = center) {
            drawLine(
                color = if (isCardinal) major else minor.copy(alpha = 0.6f),
                start = Offset(center.x, center.y - radius + 4.dp.toPx()),
                end = Offset(center.x, center.y - radius + 4.dp.toPx() + length),
                strokeWidth = if (isCardinal) 2.5.dp.toPx() else 1.dp.toPx(),
            )
        }
    }
}

/** Les lettres restent droites : seule leur position tourne avec le cadran. */
private fun DrawScope.drawCardinals(
    center: Offset,
    radius: Float,
    heading: Double,
    cardinals: List<Pair<Double, String>>,
    textMeasurer: TextMeasurer,
    style: TextStyle,
    colors: ColorScheme,
) {
    val labelRadius = radius - 34.dp.toPx()
    cardinals.forEach { (angle, label) ->
        val isNorth = angle == 0.0
        val measured = textMeasurer.measure(
            text = label,
            style = style.copy(
                color = if (isNorth) colors.primary else colors.onSurfaceVariant,
                fontWeight = if (isNorth) FontWeight.SemiBold else FontWeight.Normal,
            ),
        )
        val theta = Math.toRadians(angle - heading)
        val x = center.x + labelRadius * sin(theta).toFloat()
        val y = center.y - labelRadius * cos(theta).toFloat()
        drawText(
            textLayoutResult = measured,
            topLeft = Offset(x - measured.size.width / 2f, y - measured.size.height / 2f),
        )
    }
}

/** Aiguille pointant la Qibla, depuis le centre vers la marque de la Kaaba. */
private fun DrawScope.drawQiblaNeedle(
    center: Offset,
    radius: Float,
    screenAngle: Double,
    color: Color,
) {
    val tip = radius - 46.dp.toPx()
    val halfWidth = 9.dp.toPx()
    rotate(degrees = screenAngle.toFloat(), pivot = center) {
        val path = Path().apply {
            moveTo(center.x, center.y - tip)
            lineTo(center.x - halfWidth, center.y + 14.dp.toPx())
            lineTo(center.x, center.y + 4.dp.toPx())
            lineTo(center.x + halfWidth, center.y + 14.dp.toPx())
            close()
        }
        drawPath(path, color = color)
    }
}

/** Petite Kaaba sur le pourtour, à l'azimut exact de la Qibla. */
private fun DrawScope.drawKaabaMarker(
    center: Offset,
    radius: Float,
    screenAngle: Double,
    background: Color,
    foreground: Color,
) {
    val badgeRadius = 15.dp.toPx()
    rotate(degrees = screenAngle.toFloat(), pivot = center) {
        val badgeCenter = Offset(center.x, center.y - radius + 18.dp.toPx())
        drawCircle(color = background, radius = badgeRadius, center = badgeCenter)
        // Cube stylisé + sa bande (kiswa)
        val side = 13.dp.toPx()
        drawRect(
            color = foreground,
            topLeft = Offset(badgeCenter.x - side / 2f, badgeCenter.y - side / 2f),
            size = Size(side, side),
        )
        drawRect(
            color = background,
            topLeft = Offset(badgeCenter.x - side / 2f, badgeCenter.y - side * 0.12f),
            size = Size(side, side * 0.22f),
        )
    }
}

/** Repère fixe au sommet : c'est lui que la marque doit rejoindre. */
private fun DrawScope.drawFixedIndex(center: Offset, radius: Float, color: Color) {
    val top = center.y - radius - 2.dp.toPx()
    val half = 7.dp.toPx()
    val path = Path().apply {
        moveTo(center.x, top + 13.dp.toPx())
        lineTo(center.x - half, top)
        lineTo(center.x + half, top)
        close()
    }
    drawPath(path, color = color)
}
