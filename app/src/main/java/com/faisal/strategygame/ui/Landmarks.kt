package com.faisal.strategygame.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.faisal.strategygame.Command
import com.faisal.strategygame.FrontierViewModel
import com.faisal.strategygame.data.*
import org.json.JSONObject

/** Original vector landmarks remain sharp when the world is magnified. */
@Composable fun LandmarkArt(kind:String,modifier:Modifier=Modifier) {
    val accent=when(kind){"training"->Color(0xFF58B7DA);"gathering"->Color(0xFF8BC76C);else->Gold}
    Box(modifier,contentAlignment=Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w=size.width;val h=size.height
            drawOval(Color.Black.copy(.3f),Offset(w*.06f,h*.73f),Size(w*.88f,h*.2f))
            drawCircle(Brush.radialGradient(listOf(accent.copy(.3f),Color.Transparent),center=Offset(w*.5f,h*.5f),radius=w*.5f),w*.5f,Offset(w*.5f,h*.5f))
            fun polygon(color:Color,vararg points:Pair<Float,Float>) {val p=Path();points.forEachIndexed{i,(x,y)->if(i==0)p.moveTo(x*w,y*h)else p.lineTo(x*w,y*h)};p.close();drawPath(p,color);drawPath(p,Bronze,style=Stroke(w*.013f))}
            polygon(Color(0xFF425958),.08f to .7f,.5f to .9f,.92f to .7f,.5f to .53f)
            polygon(Color(0xFF8A9B8D),.24f to .35f,.5f to .48f,.5f to .8f,.24f to .67f)
            polygon(Color(0xFF516C6D),.5f to .48f,.76f to .35f,.76f to .67f,.5f to .8f)
            polygon(accent,.2f to .36f,.5f to .1f,.8f to .36f,.5f to .5f)
            drawLine(Bronze,Offset(w*.5f,h*.1f),Offset(w*.5f,h*.01f),w*.028f)
            polygon(accent,.51f to .02f,.73f to .07f,.51f to .13f)
            drawRoundRect(Ink,Offset(w*.36f,h*.52f),Size(w*.14f,h*.22f),androidx.compose.ui.geometry.CornerRadius(w*.04f))
            repeat(3){drawCircle(Gold,w*.024f,Offset(w*(.3f+it*.2f),h*.77f))}
        }
        Icon(when(kind){"training"->Icons.Default.Bolt;"gathering"->Icons.Default.Grass;else->Icons.Default.Shield},null,tint=Parchment,modifier=Modifier.fillMaxSize(.22f).offset(y=(-3).dp))
    }
}
fun landmarkEffect(landmark:JSONObject)=when(landmark.optString("kind")) {
    "training"->"سرعة التدريب +${landmark.optInt("bonus")}%"
    "gathering"->"سرعة الجمع +${landmark.optInt("bonus")}%"
    else->"قوة الحاكم +${num(landmark.optLong("bonus"))} نقطة"
}
@Composable fun LandmarkDetails(vm:FrontierViewModel,landmark:JSONObject,ask:(Command)->Unit) {
    val claimed=vm.doc("/expansion/v1/state").optJSONArray("claimed_landmarks")
    val owned=(0 until (claimed?.length()?:0)).any{claimed?.optInt(it)==landmark.optInt("id")}
    val cost=landmark.optLong("cost")
    val required=landmark.optInt("castle_level")
    val castle=vm.doc("/game/buildings").rows("buildings").firstOrNull{it.optString("type")=="castle"}?.optInt("level")?:0
    Text(landmarkEffect(landmark),fontWeight=FontWeight.Bold,color=Gold)
    Text(if(landmark.optString("kind")=="power") "يضيف نقاطًا ثابتة إلى قوة حاكمك مرة واحدة." else "مكافأة دائمة للأعمال الجديدة بعد التفعيل. تتجمع مكافآت المعالم من النوع نفسه.")
    Text("التفعيل خاص بحسابك ويظل متاحًا لبقية الحكّام.",style=MaterialTheme.typography.bodySmall,color=Mint)
    if(owned) Text("مفعّل ✓",color=Mint,fontWeight=FontWeight.Bold)
    else {
        Text("يتطلب قلعة مستوى $required • مستواك $castle",color=if(castle>=required) Mint else Gold)
        ResourceCosts(mapOf("food" to cost,"wood" to cost),vm.me.obj("resources"))
        ActionButton("تفعيل المعلم",vm.canExpand&&castle>=required&&vm.me.obj("resources").optLong("food")>=cost&&vm.me.obj("resources").optLong("wood")>=cost) {
            ask(Command("تفعيل ${landmark.optString("name")}","/expansion/v1/claim-landmark",json("landmark_id" to landmark.getInt("id")),details="${landmarkEffect(landmark)}\nالتكلفة: ${num(cost)} غذاء و${num(cost)} خشب. تفعيل دائم مرة واحدة."))
        }
    }
}
