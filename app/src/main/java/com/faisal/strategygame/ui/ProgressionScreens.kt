package com.faisal.strategygame.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.faisal.strategygame.*
import com.faisal.strategygame.R
import com.faisal.strategygame.data.*
import org.json.JSONObject

fun heroArt(key: String) = when(key) { "royal_marshal"->R.drawable.hero_royal; "eagle_eye"->R.drawable.hero_eagle; "storm_rider"->R.drawable.hero_storm; else->R.drawable.hero_iron }
fun heroName(key: String) = when(key) { "royal_marshal"->"المشير الملكي"; "eagle_eye"->"عين الصقر"; "storm_rider"->"فارس العاصفة"; "iron_guard"->"الحارس الحديدي"; else->title(key) }
fun troopArt(type: String) = when(type) { "cavalry"->R.drawable.unit_cavalry; "archers"->R.drawable.unit_archers; else->R.drawable.unit_infantry }
private fun effectName(key: String) = when { key.contains("attack")->"الهجوم";key.contains("defense")->"الدفاع";key.contains("speed")->"سرعة المسير";key.contains("gather")->"الجمع";else->title(key) }

@Composable
fun ArtworkBanner(image: Int, heading: String, subtitle: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(280.dp).clip(RoundedCornerShape(20.dp))) {
        Image(painterResource(image),heading,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent,Ink.copy(.15f),Ink))))
        Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
            Text(subtitle,color=Gold,fontSize=12.sp,fontWeight=FontWeight.Bold)
            Text(heading,color=Color.White,fontSize=28.sp,fontWeight=FontWeight.Black)
        }
    }
}

@Composable
fun HeroesScreen(vm: FrontierViewModel, ask: (Command)->Unit) {
    var selected by rememberSaveable { mutableStateOf("iron_guard") }
    var gear by rememberSaveable { mutableStateOf(false) }
    val commanders=vm.doc("/commanders").rows("commanders")
    val current=commanders.firstOrNull {it.optString("key")==selected} ?: commanders.firstOrNull()
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        FilterChip(!gear,{gear=false},label={Text("قاعة الأبطال")})
        FilterChip(gear,{gear=true},label={Text("تجهيزات الجيش")})
    }
    if(!gear) {
        if(current==null) { Text("جارٍ تحميل الأبطال…",color=Mint); return }
        val key=current.optString("key"); val owned=current.optBoolean("owned")
        ArtworkBanner(heroArt(key),heroName(key),if(owned) "قائد في مملكتك • المستوى ${current.optInt("level")}" else "قائد متاح للتجنيد")
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            commanders.forEach { c -> val k=c.optString("key")
                Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).border(if(k==key) 2.dp else 1.dp,if(k==key) Gold else Slate,RoundedCornerShape(12.dp)).clickable {selected=k}.padding(3.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                    Image(painterResource(heroArt(k)),heroName(k),Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(9.dp)),contentScale=ContentScale.Crop)
                    Text(heroName(k),fontSize=10.sp,maxLines=1)
                    Text(if(c.optBoolean("owned")) "مُجنّد" else "مقفل",color=if(c.optBoolean("owned")) Mint else Gold,fontSize=10.sp)
                }
            }
        }
        Panel {
            Text("خصائص القائد",fontWeight=FontWeight.Bold,color=Gold)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly) {
                Stat("الهجوم","+${current.optInt("attack_bonus_percent")}%")
                Stat("الدفاع","+${current.optInt("defense_bonus_percent")}%")
                Stat("المستوى",current.optInt("level").toString())
            }
            if(!owned) {val cost=current.optLong("recruit_cost_gold")
                ActionButton("تجنيد • ${num(cost)} ذهب",vm.canAct&&vm.me.obj("resources").optLong("gold")>=cost) {
                    ask(Command("تجنيد ${heroName(key)}","/commanders/recruit",json("key" to key),details="التكلفة: ${num(cost)} ذهب"))
                }
            }
        }
        val skills=vm.doc("/commanders/skills").rows("skills").filter {it.optString("commander_key")==key}
        Text("تطوير مهارات البطل",fontSize=22.sp,fontWeight=FontWeight.Bold,color=Gold)
        skills.forEach { s -> val level=if(owned) maxOf(1,s.optInt("level")) else 0; val maximum=s.optInt("max_level"); val cost=s.optLong("upgrade_gold")*maxOf(1,level)
            Panel {
                Row(verticalAlignment=Alignment.CenterVertically) { Icon(Icons.Default.AutoAwesome,null,tint=Gold);Spacer(Modifier.width(10.dp));Text(title(s.optString("skill_key")),fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));Text("$level / $maximum",color=Mint) }
                Text("${effectName(s.optString("effect_type"))} +${s.optInt("bonus_per_level")*level}%",color=Mint)
                LinearProgressIndicator(progress={if(maximum>0) level.toFloat()/maximum else 0f},modifier=Modifier.fillMaxWidth())
                Text("الزيادة التالية +${s.optInt("bonus_per_level")}% • ${num(cost)} ذهب",style=MaterialTheme.typography.bodySmall)
                ActionButton(if(!owned) "يتطلب تجنيد البطل" else if(level>=maximum) "بلغ الحد الأقصى" else "ترقية المهارة إلى ${level+1}",vm.canAct&&owned&&level<maximum&&vm.me.obj("resources").optLong("gold")>=cost) {
                    ask(Command("تطوير مهارة ${heroName(key)}","/commanders/skills/upgrade",json("commander_key" to key,"skill_key" to s.getString("skill_key")),details="${title(s.optString("skill_key"))}\nالمستوى $level ← ${level+1}\n${num(cost)} ذهب"))
                }
            }
        }
        if(skills.isEmpty()) Text("لا توجد مهارات متاحة لهذا القائد حاليًا.",color=Mint)
    } else {
        ArtworkBanner(R.drawable.hero_royal,"دار الحدادة","اصنع التجهيزات وارفع خصائص جيشك")
        vm.doc("/equipment").rows("equipment").forEach { e ->
            val owned=e.optBoolean("owned");val equipped=e.optBoolean("equipped");val cost=e.optLong("craft_gold");val stone=e.optLong("craft_stone")
            Panel {
                Text(title(e.optString("key")),fontSize=18.sp,fontWeight=FontWeight.Bold,color=Gold)
                Text("هجوم +${e.optInt("attack_bonus_percent")}% · دفاع +${e.optInt("defense_bonus_percent")}% · سرعة +${e.optInt("march_speed_percent")}%",style=MaterialTheme.typography.bodySmall)
                if(!owned) Text("${num(cost)} ذهب · ${num(stone)} حجر",color=Mint)
                ActionButton(if(equipped) "مُجهّز" else if(owned) "تجهيز" else "صناعة",vm.canAct&&!equipped&&(owned||(vm.me.obj("resources").optLong("gold")>=cost&&vm.me.obj("resources").optLong("stone")>=stone))) {
                    ask(Command(if(owned) "تجهيز ${e.optString("name")}" else "صناعة ${e.optString("name")}",if(owned) "/equipment/equip" else "/equipment/craft",json("equipment_key" to e.getString("key")),details=if(owned) "يستبدل التجهيز الحالي في الخانة نفسها." else "${num(cost)} ذهب · ${num(stone)} حجر"))
                }
            }
        }
    }
}
@Composable private fun Stat(label:String,value:String) {Column(horizontalAlignment=Alignment.CenterHorizontally) {Text(value,fontSize=21.sp,fontWeight=FontWeight.Bold,color=Gold);Text(label,fontSize=12.sp)} }

@Composable
fun MilitaryScreen(vm: FrontierViewModel,ask:(Command)->Unit) {
    var type by rememberSaveable {mutableStateOf("infantry")};var tier by rememberSaveable {mutableIntStateOf(1)}
    var amount by rememberSaveable {mutableStateOf("100")}; var support by rememberSaveable {mutableStateOf(false)}
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {FilterChip(!support,{support=false},label={Text("تدريب القوات")});FilterChip(support,{support=true},label={Text("العلاج والأبحاث")})}
    if(support) {ArmySupport(vm,ask);return}
    ArtworkBanner(troopArt(type),title(type),"متاح في المدينة: ${num(vm.me.obj("army").optLong(type))} جندي")
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {listOf("infantry","cavalry","archers").forEach { t ->
        FilterChip(type==t,{type=t},label={Text(title(t))},modifier=Modifier.weight(1f))
    } }
    val building=trainingBuilding(type)
    val level=vm.doc("/game/buildings").rows("buildings").firstOrNull{it.optString("type")==building}?.optInt("level") ?: 0
    Panel {
        Text("مسار تطوير ${title(type)}",fontWeight=FontWeight.Bold,color=Gold)
        Text("${title(building)} • المستوى الحالي $level",style=MaterialTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)) {(1..5).forEach {t ->
            FilterChip(tier==t,{tier=t},label={Text("T$t",fontSize=12.sp)},modifier=Modifier.weight(1f))
        } }
        TierRequirements(tier,level,building)
        val count=amount.toLongOrNull()?:0
        val cost=if(count in 1..100000) trainingCost(tier,count) else null
        OutlinedTextField(amount,{amount=it.filter(Char::isDigit).take(6)},Modifier.fillMaxWidth(),label={Text("عدد الجنود • 1 إلى 100,000")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),singleLine=true)
        val res=vm.me.obj("resources");val unlocked=level>=tierBuildingLevel(tier)
        val enough=cost!=null&&res.optLong("food")>=cost.food&&res.optLong("wood")>=cost.wood&&res.optLong("gold")>=cost.gold
        cost?.let {Text("${num(it.food)} غذاء · ${num(it.wood)} خشب · ${num(it.gold)} ذهب",color=Mint);Text("مدة التدريب: ${countdown(it.seconds*1000,0)}",style=MaterialTheme.typography.bodySmall)}
        val queue=vm.jobs.any{it.kind=="train"}
        ActionButton(when { !unlocked->"يتطلب ${title(building)} مستوى ${tierBuildingLevel(tier)}";queue->"التدريب الحالي لم يُستلم";!enough->"تحقق من العدد والموارد";else->"تدريب ${num(count)} • T$tier"},vm.canAct&&unlocked&&enough&&!queue) {
            ask(Command("تدريب ${num(count)} ${title(type)} T$tier","/game/training/start",json("type" to type,"tier" to tier,"amount" to count),"train",details="${num(cost!!.food)} غذاء · ${num(cost.wood)} خشب · ${num(cost.gold)} ذهب\nالمدة ${countdown(cost.seconds*1000,0)}"))
        }
        if(!unlocked) TextButton({vm.selectTab("city")}) {Text("الذهاب إلى المدينة للتطوير")}
        TextButton({ask(Command("استلام التدريب المكتمل","/game/training/claim",JSONObject()))},enabled=vm.canAct){Text("استلام تدريب سابق")}
    }
    Panel {
        Text("متطلبات جميع الفئات",fontWeight=FontWeight.Bold,color=Gold)
        Text("الفئة   •   مستوى المبنى   •   غذاء / خشب / ذهب للجندي",fontSize=11.sp)
        (1..5).forEach { t ->val c=trainingCost(t,1);Text("T$t     •     ${tierBuildingLevel(t)}     •     ${c.food} / ${c.wood} / ${c.gold}",color=if(t==tier) Gold else Color.White,fontSize=14.sp)}
        Text("الجيش المتاح يُعرض كإجمالي لكل نوع. تطوير القوات هنا يتم بفتح فئات تدريب جديدة.",style=MaterialTheme.typography.bodySmall,color=Mint)
    }
}
@Composable fun TierRequirements(tier:Int,level:Int,building:String) {
    val required=tierBuildingLevel(tier)
    Row(verticalAlignment=Alignment.CenterVertically) {Icon(if(level>=required) Icons.Default.CheckCircle else Icons.Default.Lock,null,tint=if(level>=required) Mint else Gold);Spacer(Modifier.width(8.dp));Text("T$tier • ${title(building)} مستوى $required",fontWeight=FontWeight.Bold)}
}

private fun productName(p:JSONObject) = when(p.optString("product_key")) {"starter_resource_crate"->"صندوق موارد البداية";"growth_bundle"->"باقة النمو";else->if(p.optString("product_type")=="iap") "${num(p.optLong("paid_gems_grant")+p.optLong("bonus_gems_grant"))} جوهرة" else p.optString("display_name")}
private fun grantsDescription(p:JSONObject):String {
    val g=p.obj("grants")
    val parts=listOf("food","wood","stone","gold","xp","bp_points")
        .filter {g.optLong(it)>0}
        .map {key ->
            val label=when(key) {"xp"->"خبرة";"bp_points"->"نقطة موسم";else->title(key)}
            "${num(g.optLong(key))} $label"
        }.toMutableList()
    val items=g.obj("items");items.keys().forEach{k->parts+="${num(items.optLong(k))} × ${title(k)}"}
    return parts.joinToString("\n")
}
@Composable
fun ShopScreen(vm:FrontierViewModel,ask:(Command)->Unit) {
    var section by rememberSaveable {mutableStateOf("offers")}
    val wallet=vm.doc("/commerce/v2/wallet");val balance=wallet.optLong("total_gems")
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Brush.linearGradient(listOf(Color(0xFF223E66),Color(0xFF172333)))).padding(22.dp)) {
        Column {Icon(Icons.Default.Diamond,null,tint=Gold,modifier=Modifier.size(44.dp));Text("خزينة المملكة",fontSize=25.sp,fontWeight=FontWeight.Bold);Text("${num(balance)} جوهرة",fontSize=32.sp,color=Gold,fontWeight=FontWeight.Black);Text("منها ${num(wallet.optLong("bonus_gems"))} جواهر مكافآت",color=Mint,fontSize=12.sp)}
    }
    Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {listOf("offers" to "الباقات","topup" to "الشحن","orders" to "مشترياتي").forEach{(key,label)->FilterChip(section==key,{section=key},label={Text(label)})}}
    if(section=="orders") {
        val orders=vm.doc("/shop/v3/orders").rows("orders")
        if(orders.isEmpty()) Panel {Text("لا توجد مشتريات بعد.",color=Mint)}
        orders.forEach{o->Panel {Text("طلب #${o.optLong("id")}",color=Gold,fontWeight=FontWeight.Bold);Text(title(o.optString("product_key")));Text("${num(o.optLong("total_gems"))} جوهرة • ${o.optInt("quantity")} باقة");Text(o.optString("created_at").take(19),fontSize=12.sp)}}
    } else {
        val topup=section=="topup"
        if(topup) Panel {Text("الشحن المالي غير مفعّل في هذه البيتا",color=Gold,fontWeight=FontWeight.Bold);Text("تظهر الباقات المتاحة، ويُفتح الدفع بعد ربط متجر Google Play والتحقق من المشتريات. لن تُخصم منك أي مبالغ هنا.",style=MaterialTheme.typography.bodySmall)}
        val products=vm.doc("/shop/v3/catalog").rows("products").filter{(it.optString("product_type")=="iap")==topup}
        if(products.isEmpty()) Text("لا توجد باقات متاحة حاليًا.",color=Mint)
        products.forEach {p ->val price=p.optLong("price_gems");val grants=grantsDescription(p)
            Panel {
                Row(verticalAlignment=Alignment.CenterVertically) {Icon(if(topup) Icons.Default.Diamond else Icons.Default.Redeem,null,tint=Gold,modifier=Modifier.size(42.dp));Spacer(Modifier.width(12.dp));Text(productName(p),fontSize=21.sp,fontWeight=FontWeight.Bold,color=Gold)}
                if(grants.isNotEmpty()) Text(grants,color=Mint)
                if(topup) {
                    Text("${num(p.optLong("paid_gems_grant"))} جوهرة + ${num(p.optLong("bonus_gems_grant"))} هدية")
                    ActionButton("الدفع غير مفعّل",false) {}
                } else ActionButton(if(balance<price) "تحتاج ${num(price)} جوهرة" else "شراء • ${num(price)} جوهرة",vm.canAct&&balance>=price&&price>0) {
                    ask(Command("شراء ${productName(p)}","/shop/v3/buy",json("product_key" to p.getString("product_key"),"quantity" to 1),details="سيُخصم ${num(price)} جوهرة من رصيدك.\n$grants"))
                }
            }
        }
    }
}
