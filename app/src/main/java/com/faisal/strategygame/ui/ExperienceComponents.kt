package com.faisal.strategygame.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.faisal.strategygame.*
import com.faisal.strategygame.R
import com.faisal.strategygame.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoyalSheet(heading:String,onDismiss:()->Unit,content:@Composable ColumnScope.()->Unit) {
    ModalBottomSheet(onDismissRequest=onDismiss,sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true),containerColor=Slate,contentColor=Parchment) {
        Column(Modifier.fillMaxWidth().heightIn(max=650.dp).verticalScroll(rememberScrollState()).padding(horizontal=18.dp).navigationBarsPadding().padding(bottom=18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Text(heading,Modifier.weight(1f),fontSize=23.sp,fontWeight=FontWeight.Black,color=Gold)
                IconButton(onDismiss){Icon(Icons.Default.Close,"إغلاق")}
            }
            content()
        }
    }
}

@Composable
fun GovernorCard(vm:FrontierViewModel) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Brush.horizontalGradient(listOf(Slate,Ink))).border(1.dp,Bronze,RoundedCornerShape(16.dp)).padding(14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
        Image(painterResource(R.drawable.hero_royal),null,Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)).border(1.dp,Gold,RoundedCornerShape(12.dp)),contentScale=ContentScale.Crop)
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
            Text(vm.me.optString("username","حاكم المملكة"),fontSize=21.sp,fontWeight=FontWeight.Black,color=Parchment,maxLines=1,overflow=TextOverflow.Ellipsis)
            Text("مستوى المدينة ${vm.me.obj("city").optInt("level",1)}",color=Mint,fontSize=12.sp)
            Text("القوة ${num(vm.me.optLong("power"))}",color=Gold,fontWeight=FontWeight.Bold,fontSize=14.sp)
        }
    }
}

@Composable
fun CouncilTile(heading:String,subtitle:String,icon:ImageVector,modifier:Modifier=Modifier,onClick:()->Unit) {
    Column(modifier.clip(RoundedCornerShape(15.dp)).background(Brush.linearGradient(listOf(Slate,Ink))).border(1.dp,Bronze.copy(.55f),RoundedCornerShape(15.dp)).clickable(onClick=onClick).padding(14.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
        Icon(icon,null,tint=Gold,modifier=Modifier.size(27.dp))
        Text(heading,fontSize=15.sp,fontWeight=FontWeight.Bold,color=Parchment)
        Text(subtitle,fontSize=10.sp,color=Mint,maxLines=2)
    }
}

@Composable
fun ActivityStrip(vm:FrontierViewModel,onOpen:()->Unit) {
    if(vm.jobs.isEmpty())return
    val ready=vm.jobs.count{vm.now>=it.ends}
    val next=vm.jobs.minByOrNull{it.ends}!!
    Row(Modifier.fillMaxWidth().background(Ink).testTag("activity-strip").clickable(onClick=onOpen).padding(horizontal=14.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        Icon(if(ready>0) Icons.Default.CheckCircle else Icons.Default.Schedule,null,tint=if(ready>0) Mint else Gold,modifier=Modifier.size(20.dp))
        Text(if(ready>0) "$ready أعمال جاهزة للاستلام" else next.label,Modifier.weight(1f),fontSize=12.sp,color=Parchment,maxLines=1,overflow=TextOverflow.Ellipsis)
        Text(if(ready>0) "استعراض" else countdown(next.ends,vm.now),color=Gold,fontSize=11.sp,fontWeight=FontWeight.Bold)
        Icon(Icons.Default.ChevronLeft,"تفاصيل الأعمال",tint=Gold,modifier=Modifier.size(18.dp))
    }
}

@Composable
fun EmptyState(heading:String,description:String,icon:ImageVector=Icons.Default.Inventory2) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Slate.copy(.6f)).padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Icon(icon,null,tint=Gold,modifier=Modifier.size(34.dp))
        Text(heading,color=Parchment,fontWeight=FontWeight.Bold)
        Text(description,color=Mint,fontSize=12.sp,textAlign=androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
fun TrainingAmountPicker(value:String,maximum:Long,onChange:(String)->Unit) {
    val count=value.toLongOrNull()?:0
    val focus=LocalFocusManager.current
    val keyboard=LocalSoftwareKeyboardController.current
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        FilledTonalIconButton({onChange((count-100).coerceIn(1,100000).toString())},enabled=count>1,modifier=Modifier.size(48.dp)){Icon(Icons.Default.Remove,"تقليل عدد الجنود")}
        OutlinedTextField(value,{onChange(it.filter(Char::isDigit).take(6))},Modifier.weight(1f).testTag("training-amount"),label={Text("عدد الجنود")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number,imeAction=ImeAction.Done),keyboardActions=KeyboardActions(onDone={focus.clearFocus();keyboard?.hide()}),singleLine=true)
        FilledTonalIconButton({onChange((count+100).coerceIn(1,100000).toString())},enabled=count<100000,modifier=Modifier.size(48.dp)){Icon(Icons.Default.Add,"زيادة عدد الجنود")}
    }
    Slider(value=count.coerceIn(0,maximum).toFloat(),onValueChange={onChange(it.toLong().coerceAtLeast(1).toString())},valueRange=0f..maximum.coerceAtLeast(1).toFloat(),enabled=maximum>0,modifier=Modifier.fillMaxWidth().testTag("training-slider"))
    Text("المتاح بمواردك: ${num(maximum)} جندي • الحد الأقصى 100,000",fontSize=11.sp,color=Mint)
}
