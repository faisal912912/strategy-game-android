package com.faisal.strategygame.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import kotlin.math.sin

val Gold = Color(0xFFE8C675)
val Ink = Color(0xFF0B1825)
val Slate = Color(0xFF192D3C)
val Mint = Color(0xFF83D8C2)

/** Original vector scenery: no remote assets, predictable rendering on low-memory phones. */
@Composable
fun CitadelScene(modifier: Modifier = Modifier, reduced: Boolean = false, level: Int = 1) {
    val motion = rememberInfiniteTransition(label = "citadel")
    val phase by motion.animateFloat(0f, 6.283f, infiniteRepeatable(tween(7000, easing = LinearEasing)), label = "wind")
    Canvas(modifier.fillMaxWidth().height(230.dp)) {
        val sx = size.width / 400f; val sy = size.height / 240f
        withTransform({ scale(sx, sy, Offset.Zero) }) {
            drawRect(Brush.verticalGradient(listOf(Color(0xFF25465C), Ink), endY = 240f), size = Size(400f, 240f))
            drawCircle(Color(0xFFFFE3A0), 25f, Offset(318f, 40f))
            for (i in 0..2) {
                val x = 50f + i * 155f + if (reduced) 0f else sin(phase + i) * 9f
                drawOval(Color.White.copy(alpha = .08f), Offset(x, 24f + i * 13), Size(80f, 13f))
            }
            ridge(listOf(0f to 130f, 70f to 60f, 120f to 104f, 202f to 42f, 305f to 115f, 380f to 67f, 400f to 100f), Color(0xFF385469))
            ridge(listOf(0f to 170f, 65f to 125f, 150f to 158f, 260f to 95f, 400f to 153f), Color(0xFF264C4F))
            drawOval(Color(0xFF183D39), Offset(-30f, 175f), Size(460f, 110f))
            val road = Path().apply { moveTo(183f, 184f); lineTo(218f, 184f); lineTo(282f, 240f); lineTo(151f, 240f); close() }
            drawPath(road, Color(0xFF8B8C78))
            drawOval(Color.Black.copy(.24f), Offset(84f, 193f), Size(238f, 20f))
            drawRect(Color(0xFFACB5AC), Offset(110f, 146f), Size(182f, 55f))
            drawRect(Color(0xFF7B9297), Offset(110f, 184f), Size(182f, 17f))
            for (i in 0..12) drawRect(Color(0xFFB7C0B6), Offset(110f+i*14f, 137f), Size(9f, 16f))
            tower(92f, 108f, 44f, 94f, false)
            tower(265f, 108f, 44f, 94f, false)
            tower(164f, 73f, 72f, 114f, true)
            drawRoundRect(Ink, Offset(184f, 164f), Size(34f, 40f), androidx.compose.ui.geometry.CornerRadius(16f, 16f))
            drawRect(Gold.copy(.5f), Offset(196f, 171f), Size(3f, 29f))
            drawLine(Gold, Offset(200f, 63f), Offset(200f, 22f), 2f)
            val flag = Path().apply {
                moveTo(201f, 24f); quadraticTo(220f, if(reduced) 30f else 30f+sin(phase)*5f, 239f, 28f)
                lineTo(231f, 39f); quadraticTo(216f, 43f, 201f, 38f); close()
            }
            drawPath(flag, Color(0xFFDC7961))
            for (i in 0..8) {
                val x = 22f+i*44f
                if (x !in 85f..315f) {
                    drawLine(Color(0xFF4A6252), Offset(x, 196f), Offset(x, 224f), 5f)
                    drawPath(Path().apply { moveTo(x, 174f); lineTo(x-15f, 211f); lineTo(x+15f, 211f); close() }, Color(0xFF437968))
                }
            }
            if (level > 1) for (i in 0..3) drawCircle(Gold.copy(.5f), 2f, Offset(144f+i*37f, 205f))
        }
    }
}
private fun DrawScope.ridge(points: List<Pair<Float,Float>>, color: Color) {
    drawPath(Path().apply { moveTo(0f,240f); points.forEach { lineTo(it.first,it.second) }; lineTo(400f,240f); close() }, color)
}
private fun DrawScope.tower(x: Float, y: Float, w: Float, h: Float, main: Boolean) {
    drawRect(Color(0xFFADBEBB), Offset(x,y), Size(w,h))
    drawRect(Color(0xFF728D95), Offset(x+w*.65f,y), Size(w*.35f,h))
    drawPath(Path().apply { moveTo(x-7,y); lineTo(x+w/2,y-(if(main) 36f else 24f)); lineTo(x+w+7,y); close() }, Color(0xFF345C79))
    for (i in 0..1) drawRoundRect(Color(0xFFEACF86), Offset(x+w*.28f,y+18f+i*25f), Size(8f,13f), androidx.compose.ui.geometry.CornerRadius(4f))
}

@Composable
fun MarchTrail(progress: Float, modifier: Modifier = Modifier) {
    val p by animateFloatAsState(progress.coerceIn(0f,1f), tween(900), label="march")
    Canvas(modifier.fillMaxWidth().height(28.dp)) {
        val y=size.height/2; val start=12f; val end=size.width-12f
        drawLine(Color.White.copy(.12f),Offset(start,y),Offset(end,y),4f)
        drawLine(Mint,Offset(start,y),Offset(start+(end-start)*p,y),4f)
        drawCircle(Gold,7f,Offset(start+(end-start)*p,y))
        drawCircle(Mint,4f,Offset(end,y))
    }
}

@Composable
fun VictoryScene(victory: Boolean, reduced: Boolean) {
    val transition=rememberInfiniteTransition(label="result")
    val phase by transition.animateFloat(0f,1f,infiniteRepeatable(tween(2400,easing=LinearEasing)),label="sparks")
    Canvas(Modifier.fillMaxWidth().height(110.dp)) {
        val center=Offset(size.width/2,size.height/2)
        drawCircle(Gold.copy(.1f),size.height*.46f,center)
        for(i in 0..11) {
            val angle=i*6.283f/12
            val distance=size.height*(.28f+(if(reduced) .2f else phase)*.22f)
            drawCircle(if(victory) Gold.copy(if(reduced) .7f else 1f-phase) else Mint.copy(.4f),3f,
                Offset(center.x+kotlin.math.cos(angle)*distance,center.y+kotlin.math.sin(angle)*distance))
        }
        val shield=Path().apply{moveTo(center.x-25,center.y-28);lineTo(center.x+25,center.y-28);lineTo(center.x+21,center.y+10);quadraticTo(center.x,center.y+38,center.x-21,center.y+10);close()}
        drawPath(shield,if(victory) Gold else Mint)
        if(victory) {drawLine(Ink,Offset(center.x-12,center.y),Offset(center.x-2,center.y+10),5f);drawLine(Ink,Offset(center.x-2,center.y+10),Offset(center.x+14,center.y-12),5f)}
    }
}
