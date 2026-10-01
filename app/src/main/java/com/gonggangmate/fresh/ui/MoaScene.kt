package com.gonggangmate.fresh.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/** An original sprout calf, drawn and animated natively for the KU campus companion. */
@Composable
fun KuruScene(modifier: Modifier = Modifier, thinking: Boolean = false, onTap: () -> Unit = {}) {
    val transition = rememberInfiniteTransition(label = "kuru")
    val breathe by transition.animateFloat(-3f, 3f,
        infiniteRepeatable(tween(if (thinking) 900 else 2200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "breathing")
    val sway by transition.animateFloat(-5f, 5f, infiniteRepeatable(tween(2500), RepeatMode.Reverse), label = "leaves")
    val blink by transition.animateFloat(1f, 1f, infiniteRepeatable(keyframes {
        durationMillis = 6000; 1f at 0; 1f at 5200; .08f at 5310; 1f at 5450; 1f at 6000
    }), label = "blink")
    Canvas(modifier.aspectRatio(1.15f).semantics { contentDescription = "새싹 뿔과 녹색 가방을 가진 송아지 쿠루. 눌러서 대화하기" }.clickable(onClick = onTap)) {
        withTransform({ scale(size.width / 360f, size.height / 313f, Offset.Zero) }) {
            drawCircle(Brush.radialGradient(listOf(Color(0xFFE1ECD5).copy(alpha = .7f), Paper.copy(alpha = 0f)), center = Offset(180f, 167f), radius = 160f), 160f, Offset(180f, 167f))
            drawOval(Color(0xFFD5DFCC).copy(alpha = .4f), Offset(100f, 276f), Size(160f, 18f))
            drawOval(Color(0xFFD5DFCC).copy(alpha = .3f), Offset(121f, 282f), Size(118f, 8f))
            translate(0f, breathe) {
                // A gentle curled tail and round, grounded silhouette.
                drawPath(Path().apply { moveTo(242f, 239f); cubicTo(292f, 251f, 288f, 210f, 276f, 211f) }, Color(0xFFC4CEAE), style = Stroke(10f, cap = StrokeCap.Round))
                drawOval(Green, Offset(268f, 203f), Size(21f, 13f))
                drawRoundRect(Color(0xFFBACAA5), Offset(139f, 241f), Size(29f, 43f), CornerRadius(13f))
                drawRoundRect(Color(0xFFBACAA5), Offset(190f, 241f), Size(29f, 43f), CornerRadius(13f))
                drawRoundRect(Color(0xFF325D49), Offset(136f, 271f), Size(33f, 13f), CornerRadius(6f))
                drawRoundRect(Color(0xFF325D49), Offset(188f, 271f), Size(34f, 13f), CornerRadius(6f))
                drawOval(Brush.verticalGradient(listOf(Color(0xFFEAF0D7), Color(0xFFCDDCB7)), startY = 178f, endY = 267f), Offset(119f, 164f), Size(122f, 108f))
                drawOval(Color(0xFFF7F8E8), Offset(147f, 184f), Size(68f, 73f))
                // Soft ears and small sprout horns.
                rotate(-25f, Offset(119f, 129f)) {
                    drawOval(Color(0xFFCFDDBA), Offset(83f, 109f), Size(57f, 33f))
                    drawOval(Color(0xFFE4CBB5), Offset(91f, 117f), Size(37f, 15f))
                }
                rotate(25f, Offset(238f, 129f)) {
                    drawOval(Color(0xFFCFDDBA), Offset(219f, 109f), Size(57f, 33f))
                    drawOval(Color(0xFFE4CBB5), Offset(232f, 117f), Size(35f, 15f))
                }
                drawRoundRect(Color(0xFFD1BD92), Offset(133f, 87f), Size(15f, 37f), CornerRadius(8f))
                drawRoundRect(Color(0xFFD1BD92), Offset(211f, 87f), Size(15f, 37f), CornerRadius(8f))
                rotate(sway, Offset(180f, 100f)) {
                    drawPath(Path().apply { moveTo(180f, 105f); quadraticBezierTo(180f, 87f, 174f, 79f) }, Green, style = Stroke(4f, cap = StrokeCap.Round))
                    drawPath(Path().apply { moveTo(177f, 89f); cubicTo(141f, 90f, 141f, 53f, 141f, 53f); cubicTo(174f, 52f, 184f, 69f, 177f, 89f) }, Color(0xFF658C51))
                    drawPath(Path().apply { moveTo(178f, 85f); cubicTo(179f, 59f, 203f, 53f, 221f, 61f); cubicTo(221f, 88f, 194f, 94f, 178f, 85f) }, Color(0xFF376F50))
                    drawLine(Color(0xFFA5BD80), Offset(152f, 65f), Offset(174f, 85f), 2f)
                    drawLine(Color(0xFF72976C), Offset(185f, 81f), Offset(209f, 65f), 2f)
                }
                drawOval(Brush.radialGradient(listOf(Color(0xFFF5F5DF), Color(0xFFD9E4C4)), center = Offset(161f, 126f), radius = 112f), Offset(104f, 96f), Size(151f, 118f))
                // Asymmetric green marking, eyes and a friendly calf muzzle.
                drawOval(Color(0xFF6F9162).copy(alpha = .86f), Offset(195f, 110f), Size(43f, 48f))
                for (x in listOf(144f, 202f)) {
                    drawOval(Ink, Offset(x, 148f - 10f * blink), Size(10f, 20f * blink))
                    if (blink > .5f) drawCircle(Color(0xFFFFFEF0), 2.1f, Offset(x + 3f, 142f))
                }
                drawOval(Color(0xFFE7B8A1).copy(alpha = .52f), Offset(125f, 161f), Size(24f, 10f))
                drawOval(Color(0xFFE7B8A1).copy(alpha = .52f), Offset(215f, 161f), Size(24f, 10f))
                drawRoundRect(Color(0xFFF0DFC3), Offset(150f, 161f), Size(60f, 38f), CornerRadius(18f))
                drawOval(Color(0xFFAB987B), Offset(161f, 172f), Size(5f, 4f))
                drawOval(Color(0xFFAB987B), Offset(192f, 172f), Size(5f, 4f))
                drawPath(Path().apply { moveTo(174f, 183f); quadraticBezierTo(180f, 189f, 186f, 183f) }, Ink, style = Stroke(2f, cap = StrokeCap.Round))
                // KU green bag, deliberately original rather than an official mascot/logo.
                drawLine(Green, Offset(136f, 198f), Offset(222f, 246f), 8f)
                drawRoundRect(Green, Offset(181f, 223f), Size(57f, 40f), CornerRadius(11f))
                drawRoundRect(Color(0xFF3D7657), Offset(180f, 220f), Size(59f, 19f), CornerRadius(8f))
                drawCircle(Sun, 4f, Offset(210f, 238f))
                drawOval(Color(0xFFD9E5C3), Offset(113f, 200f), Size(26f, 49f))
                drawOval(Color(0xFFD9E5C3), Offset(228f, 200f), Size(25f, 49f))
            }
            // Quiet floating accents reinforce an airy, character-first layout.
            drawCircle(Sun.copy(alpha = .55f), 3f, Offset(79f, 132f + sway))
            drawCircle(Green.copy(alpha = .22f), 4f, Offset(285f, 169f - sway))
            drawCircle(Color(0xFF9DB68C).copy(alpha = .6f), 2f, Offset(263f, 86f + breathe))
        }
    }
}
