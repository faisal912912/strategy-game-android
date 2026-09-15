package com.faisal.strategygame.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.faisal.strategygame.R
import com.faisal.strategygame.data.*
import org.json.JSONObject
import java.util.Locale

val Parchment=Color(0xFFF7E9C4)
val Bronze=Color(0xFF98764C)
val Emerald=Color(0xFF238678)
fun compact(n:Long):String = when {
    n>=1_000_000_000 -> String.format(Locale.US,"%.1fB",n/1_000_000_000.0)
    n>=1_000_000 -> String.format(Locale.US,"%.1fM",n/1_000_000.0)
    n>=10_000 -> String.format(Locale.US,"%.1fK",n/1_000.0)
    else -> num(n)
}
fun resourceIcon(key:String):ImageVector=when(key){"food"->Icons.Default.Grass;"wood"->Icons.Default.Forest;"stone"->Icons.Default.Terrain;else->Icons.Default.Paid}

@Composable
fun RealmHud(name:String,power:Long,resources:JSONObject,level:Int,syncing:Boolean,onProfile:()->Unit,onResources:()->Unit,onRefresh:()->Unit) {
    Column(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Ink,Ink.copy(.93f),Ink.copy(.68f),Color.Transparent))).statusBarsPadding().padding(start=10.dp,end=10.dp,top=6.dp,bottom=18.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(56.dp).clip(RoundedCornerShape(13.dp)).border(2.dp,Gold,RoundedCornerShape(13.dp)).clickable(onClick=onProfile)) {
                Image(painterResource(R.drawable.hero_royal),"ملف الحاكم",Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
                Text(level.toString(),Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Ink.copy(.9f)),color=Gold,fontWeight=FontWeight.Bold,fontSize=10.sp,textAlign=androidx.compose.ui.text.style.TextAlign.Center)
            }
            Column(Modifier.weight(1f)) {
                Text(name.ifEmpty{"حاكم المملكة"},color=Parchment,fontSize=16.sp,fontWeight=FontWeight.Black,maxLines=1,overflow=TextOverflow.Ellipsis)
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Bolt,null,tint=Gold,modifier=Modifier.size(17.dp))
                    Text(num(power),color=Gold,fontWeight=FontWeight.Bold,fontSize=13.sp)
                    Text("• المملكة 1",color=Parchment.copy(.7f),fontSize=10.sp)
                }
            }
            IconButton(onRefresh,enabled=!syncing,modifier=Modifier.size(42.dp).border(1.dp,Bronze.copy(.6f),CircleShape)) {
                if(syncing) CircularProgressIndicator(Modifier.size(19.dp),color=Gold,strokeWidth=2.dp) else Icon(Icons.Default.Sync,"تحديث المدينة",tint=Gold)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp)).background(Ink.copy(.85f)).border(1.dp,Bronze.copy(.5f),RoundedCornerShape(9.dp)).clickable(onClick=onResources).padding(vertical=6.dp),horizontalArrangement=Arrangement.SpaceEvenly) {
            listOf("food","wood","stone","gold").forEach {k ->
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(3.dp)) {
                    Icon(resourceIcon(k),title(k),tint=if(k=="wood") Mint else Gold,modifier=Modifier.size(18.dp))
                    Text(compact(resources.optLong(k)),color=Parchment,fontSize=12.sp,fontWeight=FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun RoyalDock(selected:String,onSelect:(String)->Unit) {
    val tabs=listOf("city" to "المدينة","world" to "المملكة","heroes" to "الأبطال","army" to "الجيش","shop" to "المتجر","more" to "الديوان")
    Column(Modifier.background(Brush.verticalGradient(listOf(Color(0xFF34404A),Color(0xFF101C29)))).navigationBarsPadding()) {
        HorizontalDivider(color=Gold.copy(.65f),thickness=1.dp)
        Row(Modifier.fillMaxWidth().padding(horizontal=4.dp,vertical=4.dp)) {
            tabs.forEach {(key,label)->
                val active=selected==key || (key=="more"&&selected=="missions")
                val rise by animateFloatAsState(if(active) 1.1f else 1f,tween(180),label="dock selection")
                Column(Modifier.weight(1f).testTag("tab-$key").clip(RoundedCornerShape(10.dp)).background(if(active) Gold.copy(.14f) else Color.Transparent).clickable{onSelect(key)}.padding(vertical=5.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                    Box(Modifier.size(39.dp).scale(rise),contentAlignment=Alignment.Center) {
                        when(key) {
                            "city"->Image(painterResource(R.drawable.castle_sprite),null,Modifier.fillMaxSize())
                            "heroes"->Image(painterResource(R.drawable.hero_iron),null,Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).border(1.dp,Gold,RoundedCornerShape(9.dp)),contentScale=ContentScale.Crop)
                            "shop"->Image(painterResource(R.drawable.treasure_chest),null,Modifier.fillMaxSize())
                            else->Icon(when(key){"world"->Icons.Default.Explore;"army"->Icons.Default.Security;else->Icons.Default.AccountBalance},null,tint=if(active) Gold else Parchment.copy(.75f),modifier=Modifier.size(30.dp))
                        }
                    }
                    Text(label,color=if(active) Gold else Parchment.copy(.8f),fontWeight=if(active) FontWeight.Bold else FontWeight.Normal,fontSize=10.sp,maxLines=1)
                    Box(Modifier.padding(top=3.dp).width(18.dp).height(2.dp).background(if(active) Gold else Color.Transparent,CircleShape))
                }
            }
        }
    }
}

@Composable
fun GameMedallion(icon:ImageVector,label:String,badge:String?=null,onClick:()->Unit) {
    Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.width(58.dp).clip(RoundedCornerShape(10.dp)).clickable(onClick=onClick).padding(vertical=4.dp)) {
        Box(contentAlignment=Alignment.Center) {
            Box(Modifier.size(44.dp).shadow(4.dp,RoundedCornerShape(13.dp)).background(Brush.linearGradient(listOf(Color(0xFF4B645D),Ink)),RoundedCornerShape(13.dp)).border(1.5.dp,Bronze,RoundedCornerShape(13.dp)),contentAlignment=Alignment.Center) {Icon(icon,null,tint=Gold,modifier=Modifier.size(27.dp))}
            if(!badge.isNullOrEmpty()) Text(badge,Modifier.align(Alignment.TopEnd).background(Color(0xFFAF5147),CircleShape).padding(horizontal=5.dp,vertical=1.dp),color=Color.White,fontSize=9.sp,fontWeight=FontWeight.Bold)
        }
        Text(label,color=Parchment,fontSize=10.sp,fontWeight=FontWeight.Bold,maxLines=1,modifier=Modifier.background(Ink.copy(.6f),RoundedCornerShape(4.dp)).padding(horizontal=3.dp))
    }
}

@Composable
fun QuestRibbon(heading:String,description:String,progress:Float,onClick:()->Unit) {
    Row(Modifier.fillMaxWidth().shadow(6.dp,RoundedCornerShape(12.dp)).clip(RoundedCornerShape(12.dp)).background(Brush.horizontalGradient(listOf(Color(0xFF253E3A),Ink))).border(1.dp,Bronze,RoundedCornerShape(12.dp)).clickable(onClick=onClick).padding(10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
        Icon(Icons.Default.Assignment,null,tint=Gold,modifier=Modifier.size(30.dp))
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(2.dp)) {
            Text(heading,color=Gold,fontSize=10.sp,fontWeight=FontWeight.Bold)
            Text(description,color=Parchment,fontSize=12.sp,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis)
            LinearProgressIndicator(progress={progress.coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth().height(3.dp).clip(CircleShape),color=Mint,trackColor=Color.White.copy(.12f))
        }
        Icon(Icons.Default.ChevronLeft,null,tint=Gold)
    }
}

@Composable
fun RoyalHeading(eyebrow:String,heading:String,subtitle:String="") {
    Column(Modifier.fillMaxWidth().padding(vertical=8.dp),verticalArrangement=Arrangement.spacedBy(3.dp)) {
        Text(eyebrow,color=Mint,fontSize=10.sp,fontWeight=FontWeight.Bold,letterSpacing=2.sp)
        Text(heading,color=Parchment,fontSize=28.sp,fontWeight=FontWeight.Black)
        if(subtitle.isNotEmpty())Text(subtitle,color=Parchment.copy(.65f),fontSize=12.sp)
        Row(Modifier.padding(top=5.dp),verticalAlignment=Alignment.CenterVertically) {Box(Modifier.size(5.dp).rotate(45f).background(Gold));Box(Modifier.width(66.dp).height(1.dp).background(Brush.horizontalGradient(listOf(Gold,Color.Transparent))))}
    }
}

@Composable
fun ResourceCosts(costs:Map<String,Long>,resources:JSONObject) {
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
        costs.forEach{(k,cost)->Column(Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(Ink.copy(.55f)).padding(vertical=8.dp),horizontalAlignment=Alignment.CenterHorizontally) {
            Icon(resourceIcon(k),title(k),tint=Gold,modifier=Modifier.size(21.dp))
            Text(compact(cost),color=if(resources.optLong(k)>=cost) Parchment else Color(0xFFFF9885),fontWeight=FontWeight.Bold,fontSize=12.sp)
            Text(if(resources.optLong(k)>=cost) "متوفر" else "ينقص ${compact(cost-resources.optLong(k))}",color=if(resources.optLong(k)>=cost) Mint else Color(0xFFFF9885),fontSize=9.sp,maxLines=1)
        }}
    }
}

@Composable
fun ChoiceTabs(options:List<Pair<String,String>>,selected:String,onSelect:(String)->Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Ink).border(1.dp,Bronze.copy(.45f),RoundedCornerShape(10.dp)).padding(4.dp),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
        options.forEach{(key,label)->Box(Modifier.weight(1f).clip(RoundedCornerShape(7.dp)).background(if(selected==key) Emerald else Color.Transparent).clickable{onSelect(key)}.padding(vertical=11.dp),contentAlignment=Alignment.Center){Text(label,color=if(selected==key) Color.White else Parchment.copy(.65f),fontSize=12.sp,fontWeight=FontWeight.Bold,maxLines=1)} }
}
