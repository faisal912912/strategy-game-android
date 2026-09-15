package com.faisal.strategygame.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.faisal.strategygame.tierBuildingLevel
import com.faisal.strategygame.R

@Composable
fun HeroShowcase(key:String,level:Int,owned:Boolean,reduced:Boolean) {
    Crossfade(key,animationSpec=tween(if(reduced) 0 else 250),label="hero portrait") {hero->
        Box(Modifier.fillMaxWidth().height(320.dp).clip(RoundedCornerShape(18.dp)).background(Ink).border(1.dp,Bronze,RoundedCornerShape(18.dp))) {
            Image(painterResource(heroArt(hero)),heroName(hero),Modifier.fillMaxSize(),contentScale=ContentScale.Crop,alignment=Alignment.TopCenter)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Ink.copy(.08f),Color.Transparent,Ink.copy(.97f)))))
            Text(if(owned) "قائد مُجنّد" else "متاح للتجنيد",Modifier.align(Alignment.TopStart).padding(12.dp).background(Ink.copy(.82f),RoundedCornerShape(6.dp)).border(1.dp,Bronze,RoundedCornerShape(6.dp)).padding(horizontal=10.dp,vertical=5.dp),color=Gold,fontSize=11.sp,fontWeight=FontWeight.Bold)
            Column(Modifier.align(Alignment.BottomStart).padding(18.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Text(when(hero){"eagle_eye"->"دقة • رماية";"storm_rider"->"اندفاع • فرسان";"royal_marshal"->"قيادة • مملكة";else->"حماية • مشاة"},color=Mint,fontSize=11.sp)
                Text(heroName(hero),color=Parchment,fontSize=28.sp,fontWeight=FontWeight.Black)
                Text(if(owned) "المستوى $level" else "أضف هذا القائد إلى مملكتك",color=Gold,fontSize=12.sp)
            }
        }
    }
}

@Composable
fun TroopShowcase(type:String,tier:Int,total:Long) {
    Box(Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(18.dp)).background(Brush.radialGradient(listOf(Color(0xFF436578),Ink))).border(1.dp,Bronze,RoundedCornerShape(18.dp))) {
        Image(painterResource(troopArt(type)),title(type),Modifier.fillMaxSize(),contentScale=ContentScale.Fit)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent,Color.Transparent,Ink.copy(.96f)))))
        Text("T$tier",Modifier.align(Alignment.TopStart).padding(12.dp).background(Emerald,RoundedCornerShape(8.dp)).border(1.dp,Gold,RoundedCornerShape(8.dp)).padding(9.dp),fontSize=20.sp,fontWeight=FontWeight.Black,color=Gold)
        Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            Text(title(type),fontSize=25.sp,fontWeight=FontWeight.Black,color=Parchment)
            Text("${num(total)} جندي متاح",color=Mint,fontSize=12.sp)
        }
    }
}

@Composable
fun TierTrack(selected:Int,buildingLevel:Int,onSelect:(Int)->Unit) {
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)) {
        (1..5).forEach{tier->val open=buildingLevel>=tierBuildingLevel(tier)
            Column(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if(selected==tier) Emerald else Ink).border(if(selected==tier) 1.5.dp else 1.dp,if(selected==tier) Gold else Bronze.copy(.5f),RoundedCornerShape(10.dp)).clickable{onSelect(tier)}.padding(vertical=9.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                Icon(if(open) Icons.Default.MilitaryTech else Icons.Default.Lock,null,tint=if(open) Gold else Parchment.copy(.4f),modifier=Modifier.size(21.dp))
                Text("T$tier",fontSize=13.sp,fontWeight=FontWeight.Bold,color=Parchment)
                Text("Lv.${tierBuildingLevel(tier)}",color=if(open) Mint else Parchment.copy(.5f),fontSize=9.sp)
            }
        }
    }
}

@Composable
fun TreasuryBanner(balance:Long,bonus:Long) {
    Box(Modifier.fillMaxWidth().height(215.dp).clip(RoundedCornerShape(18.dp)).background(Brush.radialGradient(listOf(Color(0xFF345C72),Ink))).border(1.dp,Bronze,RoundedCornerShape(18.dp))) {
        Image(painterResource(R.drawable.treasure_chest),null,Modifier.align(Alignment.CenterEnd).width(205.dp).height(210.dp))
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color.Transparent,Ink.copy(.9f)))))
        Column(Modifier.align(Alignment.CenterStart).padding(18.dp)) {
            Text("خزينة المملكة",fontSize=12.sp,color=Mint,fontWeight=FontWeight.Bold)
            Text(compact(balance),fontSize=37.sp,color=Gold,fontWeight=FontWeight.Black)
            Text("جوهرة",fontSize=15.sp,color=Parchment)
            Text("${compact(bonus)} جواهر مكافآت",fontSize=10.sp,color=Mint)
        }
    }
}
