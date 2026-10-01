package com.gonggangmate.fresh.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Original vector character; no external images, network or bitmap scaling required. */
@Composable
fun MoaScene(scarf: Boolean, modifier: Modifier = Modifier, onTap: () -> Unit) {
    val motion = rememberInfiniteTransition(label = "moa")
    val bob = motion.animateFloat(-2f, 2f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "breathing")
    val tail = motion.animateFloat(-8f, 8f, infiniteRepeatable(tween(1300), RepeatMode.Reverse), label = "tail")
    val blink = motion.animateFloat(1f, 1f, infiniteRepeatable(keyframes {
        durationMillis = 5200
        1f at 0; 1f at 4550; 0.08f at 4680; 1f at 4800; 1f at 5200
    }), label = "blink")

    Canvas(modifier.aspectRatio(360f / 360f).semantics { contentDescription = "보라색 고양이 모아가 방 안에서 기다리고 있어요. 눌러서 인사하기" }.clickable(onClick = onTap)) {
        withTransform({ scale(size.width / 360f, size.height / 360f, Offset.Zero) }) {
            drawRect(Brush.verticalGradient(listOf(Color(0xFFF0EBF9), Color(0xFFE8DFF2)), endY = 270f), size = Size(360f, 270f))
            drawRect(Color(0xFFF4EEE7), Offset(0f, 270f), Size(360f, 90f))
            drawLine(Color(0xFFDDD0DC), Offset(0f, 270f), Offset(360f, 270f), 2f)
            // Soft daylight window.
            drawRoundRect(Color(0xFFDDD2E8), Offset(246f, 27f), Size(88f, 115f), CornerRadius(32f))
            drawRoundRect(Color(0xFFFAF8FF), Offset(252f, 33f), Size(76f, 103f), CornerRadius(28f))
            drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFD5E8ED), Color(0xFFF2F4F0))), Offset(257f, 38f), Size(66f, 93f), CornerRadius(24f))
            drawCircle(Color(0xFFFFEDC3), 13f, Offset(304f, 63f))
            drawLine(Color.White, Offset(290f, 38f), Offset(290f, 131f), 4f)
            drawLine(Color.White, Offset(257f, 90f), Offset(323f, 90f), 4f)
            // Framed little art.
            drawRoundRect(Color(0xFFCEBCD8), Offset(122f, 25f), Size(62f, 69f), CornerRadius(6f))
            drawRoundRect(Color(0xFFFBF6EC), Offset(127f, 30f), Size(52f, 59f), CornerRadius(3f))
            drawCircle(Color(0xFFE8C77C), 10f, Offset(153f, 47f))
            drawPath(Path().apply { moveTo(130f, 82f); lineTo(144f, 61f); lineTo(157f, 74f); lineTo(171f, 59f); lineTo(176f, 82f); close() }, Color(0xFF94AE8F))
            // Plant and low bench.
            drawRoundRect(Color(0xFFDACCC0), Offset(19f, 218f), Size(74f, 53f), CornerRadius(12f))
            drawRoundRect(Color(0xFFF8EADB), Offset(15f, 211f), Size(81f, 16f), CornerRadius(8f))
            drawLine(Color(0xFF6B8970), Offset(51f, 211f), Offset(51f, 143f), 4f)
            drawOval(Color(0xFF819E7A), Offset(28f, 155f), Size(27f, 15f))
            drawOval(Color(0xFF95AF8D), Offset(48f, 144f), Size(26f, 15f))
            drawOval(Color(0xFF819E7A), Offset(46f, 176f), Size(29f, 14f))
            drawPath(Path().apply { moveTo(33f, 191f); lineTo(70f, 191f); lineTo(65f, 213f); lineTo(38f, 213f); close() }, Color(0xFFCBA28E))
            // Beanbag.
            drawOval(Color(0xFFD1DCBF), Offset(273f, 229f), Size(71f, 53f))
            drawOval(Color(0xFFE3EBCC), Offset(279f, 225f), Size(59f, 39f))
            // Rug and character.
            drawOval(Color(0xFFE1D6E9), Offset(72f, 285f), Size(215f, 46f))
            drawOval(Color(0xFFF2E9F7), Offset(78f, 286f), Size(203f, 38f))
            drawOval(Color(0xFFC9B7D5), Offset(132f, 303f), Size(98f, 16f))
            translate(82f, 97f + bob.value) { drawMoa(scarf, tail.value, blink.value) }
        }
    }
}

private fun DrawScope.drawMoa(scarf: Boolean, tail: Float, blink: Float) {
    val fur = Color(0xFF9A84C4)
    val shade = Color(0xFF7F68AD)
    val light = Color(0xFFAE9AD5)
    // Curled swishing tail.
    drawPath(Path().apply {
        moveTo(130f, 180f); cubicTo(180f + tail, 200f, 182f + tail, 147f, 160f + tail, 152f)
    }, shade, style = Stroke(18f, cap = StrokeCap.Round))
    drawOval(shade, Offset(56f, 179f), Size(35f, 39f))
    drawOval(shade, Offset(104f, 179f), Size(35f, 39f))
    drawOval(fur, Offset(47f, 100f), Size(99f, 109f))
    drawOval(Color(0xFFE7DDF5), Offset(67f, 131f), Size(59f, 66f))
    drawOval(light, Offset(33f, 117f), Size(30f, 60f))
    drawOval(fur, Offset(132f, 117f), Size(28f, 60f))
    // Ears belong to an original round lilac kitten.
    drawPath(Path().apply { moveTo(35f, 57f); cubicTo(15f, 14f, 20f, -8f, 66f, 25f); close() }, shade)
    drawPath(Path().apply { moveTo(135f, 25f); cubicTo(179f, -8f, 179f, 18f, 157f, 62f); close() }, shade)
    drawPath(Path().apply { moveTo(36f, 42f); lineTo(32f, 12f); lineTo(58f, 32f); close() }, Color(0xFFE0B2CF))
    drawPath(Path().apply { moveTo(141f, 31f); lineTo(166f, 12f); lineTo(156f, 47f); close() }, Color(0xFFE0B2CF))
    drawOval(Brush.radialGradient(listOf(light, fur), center = Offset(78f, 48f), radius = 100f), Offset(23f, 20f), Size(151f, 114f))
    drawPath(Path().apply { moveTo(78f, 23f); quadraticBezierTo(81f, 11f, 92f, 10f); quadraticBezierTo(91f, 20f, 95f, 24f) }, light)
    // Eye lids blink using the same shape rather than jumping frames.
    for (x in listOf(58f, 120f)) {
        val height = 31f * blink
        drawOval(Color(0xFFFDFBFF), Offset(x, 73f - height / 2f), Size(23f, height))
        drawOval(Ink, Offset(x + 5f, 73f - height * .38f), Size(12f, height * .76f))
        if (blink > .5f) drawCircle(Color.White, 3f, Offset(x + 10f, 67f))
    }
    drawOval(Color(0xFFD9A8C6), Offset(40f, 89f), Size(22f, 10f))
    drawOval(Color(0xFFD9A8C6), Offset(140f, 89f), Size(20f, 10f))
    drawOval(Color(0xFFF2EAF7), Offset(78f, 82f), Size(42f, 31f))
    drawPath(Path().apply { moveTo(91f, 87f); quadraticBezierTo(98f, 83f, 105f, 87f); lineTo(98f, 94f); close() }, Color(0xFF9B658C))
    drawPath(Path().apply { moveTo(98f, 94f); lineTo(98f, 99f); quadraticBezierTo(89f, 106f, 86f, 98f); moveTo(98f, 99f); quadraticBezierTo(107f, 106f, 111f, 98f) }, Ink, style = Stroke(1.8f, cap = StrokeCap.Round))
    if (scarf) {
        drawRoundRect(Color(0xFFCFDDA9), Offset(55f, 121f), Size(91f, 16f), CornerRadius(8f))
        drawRoundRect(Color(0xFFBDCD92), Offset(115f, 128f), Size(17f, 33f), CornerRadius(5f))
        drawCircle(Color(0xFFF6F6D9), 5f, Offset(97f, 129f))
    }
    drawOval(Color(0xFFEAE0F6), Offset(54f, 204f), Size(36f, 13f))
    drawOval(Color(0xFFEAE0F6), Offset(104f, 204f), Size(36f, 13f))
}
