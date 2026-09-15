package com.faisal.strategygame.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.faisal.strategygame.*
import com.faisal.strategygame.R
import com.faisal.strategygame.data.*
import org.json.JSONObject

@Composable
fun WarehouseScreen(vm:FrontierViewModel,ask:(Command)->Unit) {
    var category by rememberSaveable {mutableStateOf("all")}
    var query by rememberSaveable {mutableStateOf("")}
    var selected by remember {mutableStateOf<JSONObject?>(null)}
    var showJobs by remember {mutableStateOf(false)}
    val items=vm.doc("/inventory").rows("items").filter{it.optLong("quantity")>0}
    val gear=vm.doc("/equipment").rows("equipment").filter{it.optBoolean("owned")}
    RoyalHeading("خزائن المملكة","المستودع","التسريعات والموارد والتجهيزات في مكان واحد")
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Brush.horizontalGradient(listOf(Slate,Ink))).padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
        Image(painterResource(R.drawable.treasure_chest),null,Modifier.size(105.dp),contentScale=ContentScale.Fit)
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)) {
            Text("${num(items.sumOf{it.optLong("quantity")})} عنصر",fontSize=23.sp,color=Gold,fontWeight=FontWeight.Black)
            Text("${gear.size} تجهيز • ${items.count{inventoryCategory(it)=="speedups"}} نوع تسريع",fontSize=12.sp,color=Mint)
            TextButton({showJobs=true}){Icon(Icons.Default.Bolt,null);Text("تسريع الأعمال")}
        }
    }
    ChoiceTabs(listOf("all" to "الكل","speedups" to "التسريعات","resources" to "الموارد","gear" to "التجهيزات","other" to "أخرى"),category){category=it}
    OutlinedTextField(query,{query=it.take(60)},Modifier.fillMaxWidth().testTag("warehouse-search"),singleLine=true,label={Text("بحث في المستودع")},leadingIcon={Icon(Icons.Default.Search,null)})
    if(category in listOf("all","resources") && query.isBlank()) {
        Text("الموارد المتاحة",color=Gold,fontWeight=FontWeight.Bold)
        listOf("food","wood","stone","gold").chunked(2).forEach {row->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            row.forEach {key->Row(Modifier.weight(1f).background(Slate,RoundedCornerShape(12.dp)).padding(12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Icon(resourceIcon(key),null,tint=Gold,modifier=Modifier.size(24.dp))
                Column {Text(title(key),fontSize=11.sp);Text(num(vm.me.obj("resources").optLong(key)),fontWeight=FontWeight.Bold,color=Mint)}
            }}
        }}
        Text("حزم الموارد تبقى في المخزون حتى تستخدمها.",fontSize=12.sp,color=Mint)
    }
    if(category in listOf("all","speedups") && query.isBlank()) {
        if(vm.expansionAvailable) {
            if(!vm.doc("/expansion/v1/state").optBoolean("starter_claimed")) ActionButton("استلام صندوق التسريعات المجاني",vm.canExpand) {
                ask(Command("استلام صندوق التسريعات","/expansion/v1/starter-claim",JSONObject(),details="صندوق مرة واحدة لكل حساب: 20 تسريع دقيقة، و10 تسريع 5 دقائق، وتسريعان ساعة، و5 لكل من البناء والتدريب والبحث."))
            }
        } else ExpansionAvailability(vm)
    }
    val filtered=items.filter {(category=="all"||inventoryCategory(it)==category) && (it.optString("name")+it.optString("item_key")).contains(query,true)}
    filtered.chunked(2).forEach {row->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
        row.forEach {item->ItemTile(item,Modifier.weight(1f)){selected=item}}
        if(row.size==1)Spacer(Modifier.weight(1f))
    }}
    val equipment=gear.filter {(category=="all"||category=="gear")&&(title(it.optString("key"))+it.optString("name")).contains(query,true)}
    equipment.forEach {item->Panel {
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Default.Shield,null,tint=Gold);Text(title(item.optString("key")),Modifier.weight(1f),fontWeight=FontWeight.Bold)
            Text(if(item.optBoolean("equipped")) "مُجهّز" else "مخزّن",color=Mint,fontSize=12.sp)
        }
        Text("هجوم +${item.optInt("attack_bonus_percent")}% • دفاع +${item.optInt("defense_bonus_percent")}%",fontSize=12.sp)
        if(!item.optBoolean("equipped")) ActionButton("تجهيز",vm.canAct){ask(Command("تجهيز ${title(item.optString("key"))}","/equipment/equip",json("equipment_key" to item.getString("key"))))}
    }}
    if(filtered.isEmpty()&&equipment.isEmpty()) EmptyState("لا توجد عناصر هنا",if(query.isBlank()) "ستظهر العناصر التي تحصل عليها في هذا القسم." else "جرّب اسمًا آخر أو غيّر القسم.")
    selected?.let {item->RoyalSheet(item.optString("name"),{selected=null}) {
        ItemTile(item,Modifier.fillMaxWidth()){}
        val speed=inventoryCategory(item)=="speedups"
        Text(if(speed) "يُخصم العنصر عند تأكيد تسريع عمل جارٍ. الوقت الزائد عن نهاية العمل لا يُسترد." else "استخدام الحزمة ينقل محتواها إلى رصيدك المتاح.",color=Mint)
        if(speed) ActionButton("اختيار عمل لتسريعه",vm.expansionAvailable){selected=null;showJobs=true}
        else if(item.optString("type") in listOf("food","wood","stone","vip","stamina")) ActionButton("استخدام عنصر واحد",vm.canAct){selected=null;ask(Command("استخدام ${item.optString("name")}","/inventory/use",json("item_key" to item.getString("item_key")),details="استخدام عنصر واحد من مخزونك."))}
        else Text("يُستخدم هذا العنصر من شاشة الميزة الخاصة به.",color=Gold)
    }}
    if(showJobs) RoyalSheet("تسريع أعمال المدينة",{showJobs=false}) {JobSpeedups(vm){showJobs=false;ask(it)}}
}

@Composable private fun ItemTile(item:JSONObject,modifier:Modifier,onClick:()->Unit) {
    val category=inventoryCategory(item)
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(Slate).border(1.dp,if(category=="speedups") Gold.copy(.7f) else Bronze.copy(.4f),RoundedCornerShape(14.dp)).clickable(onClick=onClick).padding(12.dp),verticalArrangement=Arrangement.spacedBy(7.dp)) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
            Icon(if(category=="speedups") Icons.Default.Bolt else if(category=="resources") resourceIcon(item.optString("type")) else Icons.Default.Inventory2,null,tint=Gold,modifier=Modifier.size(36.dp))
            Text("×${num(item.optLong("quantity"))}",color=Mint,fontWeight=FontWeight.Bold)
        }
        Text(item.optString("name",title(item.optString("item_key"))),fontSize=13.sp,fontWeight=FontWeight.Bold,minLines=2)
        if(category=="speedups")Text(countdown(item.optLong("effect_value")*1000,0),fontSize=12.sp,color=Gold)
        else if(category=="resources")Text("${num(item.optLong("effect_value"))} ${title(item.optString("type"))}",fontSize=12.sp,color=Gold)
    }
}

@Composable fun ExpansionAvailability(vm:FrontierViewModel) {
    Text(if(vm.expansionError) "تعذر تحديث حالة التوسعة. أعد المحاولة." else "التسريعات ومعالم المملكة تحتاج تفعيل تحديث العالم على السيرفر.",color=Gold,fontSize=12.sp)
    TextButton({vm.checkExpansion()}){Text("التحقق من التحديث")}
}

@Composable fun JobSpeedups(vm:FrontierViewModel,ask:(Command)->Unit) {
    var selected by remember {mutableStateOf<CityJob?>(null)}
    if(!vm.expansionAvailable) {ExpansionAvailability(vm);return}
    val jobs=vm.jobs.filter{it.kind in listOf("build","train","research")&&it.jobId>0}
    if(jobs.isEmpty())EmptyState("لا يوجد عمل جارٍ","ابدأ بناءً أو تدريبًا أو بحثًا لتستخدم التسريعات.")
    jobs.forEach {job->Panel {
        Text(jobName(job),color=Gold,fontWeight=FontWeight.Bold)
        Text(countdown(job.ends,vm.now),color=Mint)
        if(job.ends>vm.now)ActionButton("تسريع",vm.canExpand){selected=job}
        else ActionButton("استلام العمل المكتمل",vm.canAct){ask(Command("استلام ${jobName(job)}",when(job.kind){"build"->"/game/buildings/claim";"train"->"/game/training/claim";else->"/game/research/claim"},JSONObject()))}
    }}
    selected?.let {old->
        val job=jobs.firstOrNull{it.kind==old.kind&&it.jobId==old.jobId}
        if(job!=null) SpeedupPicker(job,vm.doc("/inventory").rows("items"),vm.now,vm.canExpand,{selected=null}){selected=null;ask(it)}
        else LaunchedEffect(Unit){selected=null}
    }
}

fun jobName(job:CityJob)=job.label.split(' ').joinToString(" "){title(it)}

@Composable fun SpeedupPicker(job:CityJob,items:List<JSONObject>,now:Long,enabled:Boolean,onDismiss:()->Unit,ask:(Command)->Unit) {
    var key by rememberSaveable(job.kind,job.jobId) {mutableStateOf("")}
    var count by rememberSaveable(job.kind,job.jobId) {mutableStateOf(1L)}
    val compatible=items.filter{speedupCompatible(it.optString("type"),job.kind)&&it.optLong("quantity")>0}
    val item=compatible.firstOrNull{it.optString("item_key")==key}?:compatible.minByOrNull{it.optLong("effect_value")}
    val remaining=(job.ends-now).coerceAtLeast(0)
    val seconds=item?.optLong("effect_value")?:0
    val maximum=speedupLimit(remaining,seconds,item?.optLong("quantity")?:0)
    val amount=count.coerceIn(1,maximum.coerceAtLeast(1))
    RoyalSheet("${jobName(job)} • تسريع",onDismiss) {
        Text("الوقت المتبقي ${countdown(job.ends,now)}",color=Mint)
        if(compatible.isEmpty()) EmptyState("لا توجد تسريعات مناسبة","احصل على صندوق التسريعات من المستودع.")
        compatible.forEach {speed->val selected=speed.optString("item_key")==item?.optString("item_key")
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if(selected) Emerald else Ink).clickable{key=speed.optString("item_key");count=1}.padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
                Icon(Icons.Default.Bolt,null,tint=Gold);Text(speed.optString("name"),Modifier.weight(1f).padding(horizontal=8.dp),fontSize=13.sp);Text("×${speed.optLong("quantity")}",color=Mint)
            }
        }
        if(item!=null&&maximum>0) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceEvenly) {
                FilledTonalIconButton({count=(amount-1).coerceAtLeast(1)},enabled=amount>1){Icon(Icons.Default.Remove,"تقليل التسريعات")}
                Text("$amount",Modifier.testTag("speedup-count"),fontSize=25.sp,color=Gold)
                FilledTonalIconButton({count=(amount+1).coerceAtMost(maximum)},enabled=amount<maximum){Icon(Icons.Default.Add,"زيادة التسريعات")}
                TextButton({count=maximum}){Text("اللازم")}
            }
            val saved=minOf(remaining,seconds*amount*1000)
            Text("اختصار ${countdown(saved,0)} • بعد التسريع ${countdown(remaining-saved,0)}",color=Mint)
            if(seconds*amount*1000>remaining)Text("وقت زائد غير مسترد: ${countdown(seconds*amount*1000-remaining,0)}",fontSize=12.sp,color=Gold)
            ActionButton("استخدام $amount تسريع",enabled){ask(speedupCommand(job,item.getString("item_key"),amount).copy(details="خصم $amount من ${item.optString("name")}\nاختصار ${countdown(saved,0)}. الوقت الزائد لا يُسترد."))}
        } else if(remaining==0L)Text("اكتمل العمل. يمكنك استلامه دون تسريع.",color=Gold)
    }
}
