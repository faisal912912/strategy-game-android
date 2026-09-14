package com.faisal.strategygame.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.faisal.strategygame.*
import com.faisal.strategygame.data.*
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale

private val labels = mapOf(
    "castle" to "القلعة", "barracks" to "ثكنة المشاة", "academy" to "الأكاديمية", "hospital" to "المستشفى",
    "farm" to "المزرعة", "lumber_mill" to "المنشرة", "quarry" to "المحجر", "gold_mine" to "منجم الذهب", "warehouse" to "المخزن",
    "stable" to "الإسطبل", "archery_range" to "ميدان الرماة", "food" to "غذاء", "wood" to "خشب", "stone" to "حجر", "gold" to "ذهب",
    "infantry" to "مشاة", "cavalry" to "فرسان", "archers" to "رماة", "outbound" to "في الطريق", "returning" to "عائدون", "gathering" to "يجمعون الموارد", "completed" to "اكتملت", "healing" to "قيد العلاج",
    "economy_gathering" to "كفاءة الجمع", "economy_storage" to "سعة التخزين", "military_archers" to "هجوم الرماة", "military_cavalry" to "هجوم الفرسان", "military_infantry" to "هجوم المشاة",
    "daily_login" to "حضورك اليومي", "daily_gather" to "اجمع الموارد", "daily_monster" to "اهزم وحشًا", "daily_donate" to "تبرع للتحالف", "daily_battle" to "معركة ضد لاعب",
    "gather_once" to "أكمل مسيرة جمع", "hunt_once" to "أكمل رحلة صيد", "heal_10" to "عالج 10 جنود", "kvk_battle" to "معركة الممالك"
)
fun title(key: String) = labels[key] ?: key.replace('_',' ')
fun num(n: Long): String = NumberFormat.getIntegerInstance(Locale.US).format(n)
fun countdown(end: Long, now: Long): String { val s=((end-now+999)/1000).coerceAtLeast(0); return if(s==0L) "جاهز" else "%02d:%02d".format(s/60,s%60) }

@Composable
fun FrontierApp(vm: FrontierViewModel = viewModel()) {
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(vm, owner) { owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        var ticks=0
        while(true) { vm.tick(); if(ticks++%20==0) vm.refresh(); delay(1000) }
    } }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        if (!vm.signedIn) Gate(vm) else Kingdom(vm)
    }
}
@Composable
private fun Gate(vm: FrontierViewModel) {
    var user by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var register by rememberSaveable { mutableStateOf(false) }
    var gateway by rememberSaveable { mutableStateOf(vm.gateway) }
    var settings by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(Ink).safeDrawingPadding().imePadding().verticalScroll(rememberScrollState())) {
        CitadelScene(reduced=vm.reduceMotion)
        Column(Modifier.padding(24.dp), verticalArrangement=Arrangement.spacedBy(14.dp)) {
            Text("حُدود المملكة", fontSize=34.sp, fontWeight=FontWeight.Black, color=Gold)
            Text("ابنِ مدينتك. جهّز جيشك. اكتشف العالم.", color=Mint)
            Text("المملكة الأولى • بيتا 0.2", style=MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                FilterChip(!register,{register=false},label={Text("دخول")},enabled=!vm.busy)
                FilterChip(register,{register=true},label={Text("حساب جديد")},enabled=!vm.busy)
            }
            OutlinedTextField(user,{user=it.take(32)},Modifier.fillMaxWidth(),label={Text("اسم الحساب")},singleLine=true,enabled=!vm.busy)
            OutlinedTextField(password,{password=it.take(128)},Modifier.fillMaxWidth(),label={Text("كلمة المرور")},supportingText={Text("10 أحرف على الأقل. احتفظ بها للعودة إلى مدينتك.")},singleLine=true,
                visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password),enabled=!vm.busy)
            vm.error?.let { Notice(it, true) }
            ActionButton(if(vm.busy) "جارٍ الاتصال…" else if(register) "أنشئ مملكتي" else "ادخل المملكة", !vm.busy && user.length>=3 && password.length>=10) {
                vm.authenticate(user,password,gateway,register)
            }
            if(vm.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            TextButton({settings=!settings},enabled=!vm.busy) { Text("إعدادات الاتصال") }
            AnimatedVisibility(settings) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    OutlinedTextField(gateway,{gateway=it},Modifier.fillMaxWidth(),label={Text("HTTPS gateway")},singleLine=true,enabled=!vm.busy)
                }
            }
        }
    }
}
@Composable
private fun Kingdom(vm: FrontierViewModel) {
    var confirmation by remember { mutableStateOf<Command?>(null) }
    var detail by remember { mutableStateOf<Pair<String,String>?>(null) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(vm.message) { vm.message?.let { snackbar.showSnackbar(it); vm.clearMessage() } }
    val ask: (Command)->Unit = { confirmation=it }
    Scaffold(containerColor=Ink, snackbarHost={SnackbarHost(snackbar)}, topBar={
        Column(Modifier.statusBarsPadding().padding(horizontal=16.dp,vertical=8.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(vm.me.optString("username"),fontWeight=FontWeight.Bold,fontSize=19.sp)
                    Text("المملكة 1  •  قوة ${num(vm.me.optLong("power"))}",color=Gold,style=MaterialTheme.typography.labelMedium)
                }
                IconButton({vm.refresh()},enabled=!vm.busy&&!vm.refreshing) { Icon(Icons.Default.Refresh,"تحديث المدينة",tint=Mint) }
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                val resources=vm.me.obj("resources")
                listOf("food","wood","stone","gold").forEach { k -> Column(horizontalAlignment=Alignment.CenterHorizontally) {
                    Text(num(resources.optLong(k)),fontWeight=FontWeight.Bold,fontSize=14.sp,color=if(k=="gold") Gold else Color.White)
                    Text(title(k),style=MaterialTheme.typography.labelSmall,color=Color(0xFF9FB3C2))
                } }
            }
            if(vm.busy||vm.refreshing) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top=8.dp))
        }
    }, bottomBar={
        NavigationBar(containerColor=Slate) {
            val tabs=listOf(Triple("city","المدينة",Icons.Default.Home),Triple("army","الجيش",Icons.Default.Shield),Triple("world","العالم",Icons.Default.Public),Triple("missions","السجل",Icons.Default.Assignment),Triple("more","المزيد",Icons.Default.Menu))
            tabs.forEach { (key,label,icon) -> NavigationBarItem(vm.tab==key,{if(!vm.busy&&!vm.refreshing) vm.selectTab(key)},icon={Icon(icon,label)},label={Text(label)},alwaysShowLabel=true) }
        }
    }) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            vm.error?.let { item { Notice(it,true) } }
            vm.pending?.let { c -> item { Panel {
                Text("بانتظار تأكيد الأمر",color=Gold,fontWeight=FontWeight.Bold)
                Text(c.title)
                Text("لم نتلقَ تأكيدًا نهائيًا. أعد التحقق من نفس الأمر قبل إصدار أمر جديد.",style=MaterialTheme.typography.bodySmall)
                ActionButton("تحقق من التنفيذ",!vm.busy&&!vm.refreshing) {vm.retryPending()}
            } } }
            if(vm.jobs.isNotEmpty()) item { Jobs(vm,ask) }
            item {
                AnimatedContent(vm.tab,label="page",transitionSpec={fadeIn() togetherWith fadeOut()}) { tab ->
                    Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                        when(tab) {
                            "city" -> City(vm,ask)
                            "army" -> Army(vm,ask)
                            "world" -> World(vm,ask)
                            "missions" -> Journal(vm,ask) { a,b -> detail=a to b }
                            else -> More(vm,ask)
                        }
                    }
                }
            }
            item { Text(if(vm.lastSync==0L) "جارٍ تحميل المدينة…" else "آخر مزامنة منذ ${((System.currentTimeMillis()-vm.lastSync)/1000).coerceAtLeast(0)} ث • بيتا 0.2",style=MaterialTheme.typography.labelSmall,color=Color(0xFF8196A6)) }
        }
    }
    confirmation?.let { command -> AlertDialog(onDismissRequest={confirmation=null},title={Text(command.title)},text={Text(commandDescription(command))},
        confirmButton={TextButton({confirmation=null;vm.submit(command)},enabled=vm.canAct) {Text("تأكيد الأمر")}},dismissButton={TextButton({confirmation=null}) {Text("إلغاء")}}) }
    detail?.let { (heading,body) -> AlertDialog(onDismissRequest={detail=null},title={Text(heading)},text={Text(body,Modifier.verticalScroll(rememberScrollState()))},confirmButton={TextButton({detail=null}){Text("إغلاق")}}) }
    vm.battleResult?.let { result -> AlertDialog(onDismissRequest={vm.dismissBattle()},title={Text(if(result.optBoolean("success")) "انتصار الحملة" else "انتهت الحملة")},text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        VictoryScene(result.optBoolean("success"),vm.reduceMotion)
        Text("خسائر ${num(result.optLong("losses"))} • جرحى ${num(result.optLong("wounded"))}")
        val rewards=result.obj("rewards")
        Text(listOf("food","wood","gold").joinToString(" · "){"${num(rewards.optLong(it))} ${title(it)}"},color=Gold)
        Text("عادت القوات الناجية. يمكنك علاج الجرحى من شاشة الجيش.",style=MaterialTheme.typography.bodySmall)
    }},confirmButton={TextButton({vm.dismissBattle()}){Text("العودة إلى المملكة")}}) }
}
private fun commandDescription(c: Command): String = when {
    c.path.endsWith("/hunt/start") || c.path.endsWith("/monsters/hunt") -> "ستغادر القوات المحددة المدينة. يحسب السيرفر النتيجة والخسائر؛ قد تحتاج القوات المصابة إلى العلاج."
    c.path.endsWith("/recall") -> "تبدأ رحلة العودة. الاستدعاء يلغي حمولة الجمع الحالية."
    c.path=="/game/buildings/upgrade" -> "ستُخصم تكلفة الترقية الموضحة. يمكنك مواصلة اللعب أثناء البناء."
    else -> "تنفيذ ${c.title} بالاختيارات والتكلفة الموضحة؟"
}
@Composable
fun Panel(content: @Composable ColumnScope.()->Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Slate).border(1.dp,Color.White.copy(.06f),RoundedCornerShape(18.dp)).padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp),content=content)
}
@Composable
private fun Notice(text: String, failure: Boolean=false) {
    Text(text,Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if(failure) Color(0xFF493034) else Slate).padding(12.dp),color=if(failure) Color(0xFFFFC7BC) else Mint,style=MaterialTheme.typography.bodySmall)
}
@Composable
fun ActionButton(text: String, enabled: Boolean=true, action: ()->Unit) {
    val interaction=remember { MutableInteractionSource() }; val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if(pressed) .97f else 1f,label="button press")
    val haptics=LocalHapticFeedback.current
    Button({haptics.performHapticFeedback(HapticFeedbackType.LongPress);action()},Modifier.fillMaxWidth().heightIn(min=48.dp).scale(scale),enabled=enabled,interactionSource=interaction,shape=RoundedCornerShape(12.dp)) { Text(text,fontWeight=FontWeight.Bold) }
}
@Composable
private fun Section(heading: String, subtitle: String="") { Text(heading,fontSize=23.sp,fontWeight=FontWeight.Bold,color=Gold);if(subtitle.isNotEmpty()) Text(subtitle,color=Color(0xFF9FB3C2),style=MaterialTheme.typography.bodySmall) }
@Composable
private fun Jobs(vm: FrontierViewModel, ask: (Command)->Unit) { Panel {
    Text("أعمال المدينة",color=Gold,fontWeight=FontWeight.Bold)
    vm.jobs.forEach { job ->
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(job.label,Modifier.weight(1f));Text(countdown(job.ends,vm.now),color=Mint)}
        if(vm.now>=job.ends) {
            val path=job.claimPath.ifEmpty { when(job.kind){"build"->"/game/buildings/claim";"train"->"/game/training/claim";else->"/game/research/claim"} }
            ActionButton("استلام ${job.label}",vm.canAct){ask(Command("استلام ${job.label}",path,if(job.claimId>0) json("id" to job.claimId) else JSONObject()))}
        }
    }
} }
@Composable
private fun City(vm: FrontierViewModel, ask: (Command)->Unit) {
    val level=vm.me.obj("city").optInt("level",1)
    Box(Modifier.clip(RoundedCornerShape(20.dp))) {
        CitadelScene(reduced=vm.reduceMotion,level=level)
        Text("مدينتك • المستوى $level",Modifier.align(Alignment.BottomStart).padding(16.dp).background(Ink.copy(.8f),RoundedCornerShape(8.dp)).padding(8.dp),fontWeight=FontWeight.Bold)
    }
    val progression=vm.doc("/city/progression")
    Panel {
        Text("رحلة الحاكم",color=Gold,fontWeight=FontWeight.Bold)
        Text("طوّر المباني، درّب القوات ثم اجمع الموارد لتمويل رحلتك التالية.")
        Text("خبرة المدينة ${num(progression.optLong("xp"))} / ${num(progression.optLong("xp_for_next_level"))}")
        LinearProgressIndicator(progress={ (progression.optDouble("xp",0.0)/progression.optDouble("xp_for_next_level",1.0).coerceAtLeast(1.0)).toFloat().coerceIn(0f,1f) },modifier=Modifier.fillMaxWidth())
        if(progression.optBoolean("can_upgrade")) ActionButton("ترقية المدينة",vm.canAct){ask(Command("ترقية المدينة","/city/progression/upgrade",JSONObject()))}
    }
    Section("حيّ البناء","اضغط على الترقية لمراجعة الأمر قبل إنفاق الموارد.")
    vm.doc("/game/buildings").rows("buildings").forEach { b ->
        val type=b.optString("type"); val next=b.optInt("level")+1
        Panel {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Icon(if(type=="castle") Icons.Default.Castle else Icons.Default.Domain,null,tint=Gold,modifier=Modifier.size(32.dp))
                Spacer(Modifier.width(12.dp));Column {Text(title(type),fontWeight=FontWeight.Bold);Text("المستوى ${b.optInt("level")}",color=Mint)}
            }
            Text("${num(next*700L)} غذاء · ${num(next*700L)} خشب · ${num(next*350L)} حجر · ${num(next*100L)} ذهب",style=MaterialTheme.typography.bodySmall)
            val res=vm.me.obj("resources")
            val enough=res.optLong("food")>=next*700 && res.optLong("wood")>=next*700 && res.optLong("stone")>=next*350 && res.optLong("gold")>=next*100
            ActionButton(if(next>30) "وصل للمستوى الأقصى" else if(!enough) "الموارد غير كافية" else "ترقية • ${next*30} ثانية",vm.canAct&&enough&&next<=30&&vm.jobs.none{it.kind=="build"}) {
                ask(Command("ترقية ${title(type)}","/game/buildings/upgrade",json("building" to type),"build"))
            }
        }
    }
    Panel {
        Text("استلام أعمال بدأت من جهاز آخر",fontWeight=FontWeight.Bold)
        Text("السيرفر يحتفظ بالأعمال. إذا لم يظهر مؤقتها هنا، اطلب استلامها بعد انتهاء وقتها.",style=MaterialTheme.typography.bodySmall)
        TextButton({ask(Command("استلام البناء المكتمل","/game/buildings/claim",JSONObject()))},enabled=vm.canAct){Text("استلام البناء")}
    }
}
@Composable
private fun Army(vm: FrontierViewModel, ask: (Command)->Unit) {
    var troop by rememberSaveable { mutableStateOf("infantry") };var amount by rememberSaveable{mutableStateOf("100")}
    Section("قوات المملكة","القوات المتاحة في المدينة؛ قوات المسيرات تظهر في شاشة العالم.")
    Panel {
        val army=vm.me.obj("army")
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) { listOf("infantry","cavalry","archers").forEach {t->Column {Text(title(t));Text(num(army.optLong(t)),color=Gold,fontSize=22.sp,fontWeight=FontWeight.Bold)} } }
        Row(Modifier.horizontalScroll(rememberScrollState())) {listOf("infantry","cavalry","archers").forEach { t->FilterChip(troop==t,{troop=t},label={Text(title(t))},modifier=Modifier.padding(end=6.dp))}}
        OutlinedTextField(amount,{amount=it.filter(Char::isDigit).take(5)},Modifier.fillMaxWidth(),label={Text("عدد القوات • الفئة الأولى")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),singleLine=true)
        val count=amount.toLongOrNull() ?: 0;val res=vm.me.obj("resources")
        Text("${num(count*10)} غذاء · ${num(count*5)} خشب · ${num(count*2)} ذهب",color=Mint,style=MaterialTheme.typography.bodySmall)
        ActionButton("تدريب ${num(count)} ${title(troop)} • ${maxOf(10,count/10)} ث",vm.canAct&&count in 1..10000&&vm.jobs.none{it.kind=="train"}&&res.optLong("food")>=count*10&&res.optLong("wood")>=count*5&&res.optLong("gold")>=count*2) {
            ask(Command("تدريب ${num(count)} ${title(troop)}","/game/training/start",json("type" to troop,"tier" to 1,"amount" to count),"train"))
        }
        TextButton({ask(Command("استلام التدريب المكتمل","/game/training/claim",JSONObject()))},enabled=vm.canAct){Text("استلام تدريب سابق")}
    }
    Section("مستشفى الحملات")
    Panel {
        val h=vm.doc("/hospital");val w=h.obj("wounded");val total=listOf("infantry","cavalry","archers").sumOf{w.optLong(it)}
        Text("الجرحى: ${num(total)} • السعة: ${num(h.optLong("capacity"))}")
        Text("العلاج: ${num(total*20)} غذاء · ${num(total*10)} خشب",color=Mint,style=MaterialTheme.typography.bodySmall)
        ActionButton("علاج جرحى الحملات",vm.canAct&&total>0&&vm.me.obj("resources").optLong("food")>=total*20&&vm.me.obj("resources").optLong("wood")>=total*10){ask(Command("علاج $total جندي","/hospital/heal",w))}
    }
    Section("مستشفى الحدود")
    Panel {
        val h=vm.doc("/hospital/v2/status");val w=h.obj("wounded");val total=listOf("infantry","cavalry","archers").sumOf{w.optLong(it)}
        Text("الجرحى: ${num(total)}  •  السعة: ${num(h.optLong("capacity"))}")
        Text("المشغول: ${num(h.optLong("occupied"))}  •  أعمال العلاج: ${h.obj("healing").optInt("active_jobs")}",style=MaterialTheme.typography.bodySmall)
        ActionButton("علاج جميع الجرحى",vm.canAct&&total>0) {ask(Command("علاج $total جندي","/hospital/v2/heal/start",w))}
        vm.doc("/hospital/v2/heal/jobs").rows("jobs").filter{it.optString("status")=="healing"}.forEach {Text("علاج • ${countdown(instantMillis(it.optString("finishes_at")),vm.now)}",color=Mint)}
    }
    Section("الأكاديمية")
    vm.doc("/game/research").rows("research").forEach {r->Panel {
        Text(title(r.optString("key")),fontWeight=FontWeight.Bold);Text("المستوى ${r.optInt("level")} / ${r.optInt("max_level")}",color=Mint)
        ActionButton("تطوير البحث",vm.canAct&&r.optInt("level")<r.optInt("max_level")&&vm.jobs.none{it.kind=="research"}){ask(Command("بحث ${title(r.optString("key"))}","/game/research/start",json("key" to r.getString("key")),"research"))}
    } }
    TextButton({ask(Command("استلام البحث المكتمل","/game/research/claim",JSONObject()))},enabled=vm.canAct){Text("استلام بحث سابق")}
    Section("القادة")
    vm.doc("/commanders").rows("commanders").forEach {c->Panel {
        Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.Person,null,tint=Gold,modifier=Modifier.size(40.dp));Column(Modifier.padding(start=12.dp)){Text(c.optString("name"),fontWeight=FontWeight.Bold);Text("هجوم +${c.optInt("attack_bonus_percent")}% · دفاع +${c.optInt("defense_bonus_percent")}%",color=Mint)}}
        if(c.optBoolean("owned")) Text("ضمن قواتك • المستوى ${c.optInt("level")}",color=Gold)
        else ActionButton("تجنيد • ${num(c.optLong("recruit_cost_gold"))} ذهب",vm.canAct&&vm.me.obj("resources").optLong("gold")>=c.optLong("recruit_cost_gold")){ask(Command("تجنيد ${c.optString("name")}","/commanders/recruit",json("key" to c.getString("key"))))}
    } }
}

@Composable
private fun World(vm: FrontierViewModel, ask: (Command)->Unit) {
    var frontier by rememberSaveable { mutableStateOf(false) }
    var x by rememberSaveable {mutableStateOf("250")};var y by rememberSaveable {mutableStateOf("250")}
    var selection by remember { mutableStateOf<JSONObject?>(null) }
    var selectionType by remember { mutableStateOf("") }
    Section("خريطة العالم","اجمع الموارد ووازن بين حجم الجيش والخطر قبل إرسال القوات.")
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        FilterChip(!frontier,{frontier=false;selection=null},label={Text("منطقة الحملات")})
        FilterChip(frontier,{frontier=true;selection=null},label={Text("الحدود الجديدة")})
    }
    if(frontier) {
        val location=vm.doc("/world/v2/state")
        Panel {
            Text("موقع مدينتك: ${location.optInt("x")}، ${location.optInt("y")}",color=Gold)
            Text("طاقة الصيد: ${vm.doc("/world/v4/pve/status").optInt("energy")} / ${vm.doc("/world/v4/pve/status").optInt("max_energy")}")
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(x,{x=it.filter(Char::isDigit).take(3)},Modifier.weight(1f),label={Text("X")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),singleLine=true)
                OutlinedTextField(y,{y=it.filter(Char::isDigit).take(3)},Modifier.weight(1f),label={Text("Y")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),singleLine=true)
            }
            ActionButton("استكشاف المنطقة",!vm.busy&&!vm.refreshing&&(x.toIntOrNull()?:-1) in 0..499&&(y.toIntOrNull()?:-1) in 0..499){vm.scan(x.toInt(),y.toInt())}
        }
        val nodes=vm.doc(vm.nodePath).rows("nodes");val monsters=vm.doc(vm.monsterPath).rows("monsters")
        if(nodes.isEmpty()&&monsters.isEmpty()) Notice("لا توجد أهداف في هذه المنطقة حاليًا. غيّر إحداثيات الاستكشاف أو العب في منطقة الحملات.")
        nodes.forEach {n->TargetCard("منبع ${title(n.optString("node_type"))}",n.optInt("level"),n.optInt("x"),n.optInt("y"),"متبقي ${num(n.optLong("remaining_amount"))}",n.optString("status")=="active"&&vm.canAct){selection=n;selectionType="gather"}}
        monsters.forEach {m->TargetCard(m.optString("name"),m.optInt("level"),m.optInt("x"),m.optInt("y"),"قوة ${num(m.optLong("power"))} · صحة ${num(m.optLong("current_hp"))}",m.optString("status")=="active"&&vm.canAct){selection=m;selectionType="hunt"}}
    } else {
        Notice("حملات الموارد والصيد متاحة الآن. عند انتهاء الرحلة، استلم النتيجة من أعمال المدينة لإعادة القوات والغنائم.")
        Section("مصادر الموارد")
        vm.doc("/world/resources").rows("nodes").filter{it.optInt("level")==1}.forEach {n->Panel {
            Text("منبع ${title(n.optString("type"))}",fontWeight=FontWeight.Bold,color=Gold)
            Text("متبقي ${num(n.optLong("amount"))} • المستوى ${n.optInt("level")}")
            ActionButton("جمع 1,000 ${title(n.optString("type"))}",vm.canAct&&n.optLong("amount")>=1000&&vm.jobs.none{it.kind=="gather"}){
                ask(Command("جمع ${title(n.optString("type"))}","/world/gather/start",json("node_id" to n.getLong("id"),"amount" to 1000),"gather"))
            }
        } }
        Section("حملات الصيد")
        vm.doc("/world/monsters").rows("monsters").forEach {m->TargetCard(if(m.optString("type")=="beast") "وحش البراري" else "معسكر الغزاة",m.optInt("level"),m.optInt("x"),m.optInt("y"),"قوة ${num(m.optLong("power"))} • غنيمة ${num(m.optLong("food_reward"))} غذاء",vm.canAct&&m.optBoolean("available")&&vm.jobs.none{it.kind=="hunt"}){selection=m;selectionType="classicHunt"}}
    }
    val gathers=vm.doc("/world/v3/gather/marches").rows("marches")
    val hunts=vm.doc("/world/v4/hunt/marches").rows("marches")
    if(gathers.isNotEmpty()||hunts.isNotEmpty()) Section("مسيرات الحدود","تكتمل العودة وحفظ النتائج تلقائيًا، حتى وأنت خارج اللعبة.")
    (gathers.map{it to false}+hunts.map{it to true}).filter{it.first.optString("status")!="completed"}.forEach { (m,hunt)->
        val status=m.optString("status")
        val end=instantMillis(m.optString(when(status){"returning"->"return_at";"gathering"->"gather_complete_at";else->"arrive_at"}))
        val seconds=m.optLong(if(status=="gathering") "gather_seconds" else "travel_seconds",1).coerceAtLeast(1)
        Panel {
            Text(if(hunt) m.optString("monster_name","رحلة صيد") else "جمع ${title(m.optString("node_type"))}",fontWeight=FontWeight.Bold)
            Text("${title(status)} • ${countdown(end,vm.now)}",color=Mint)
            MarchTrail(1f-(end-vm.now).coerceAtLeast(0)/(seconds*1000f))
            Text("${num(listOf("infantry","cavalry","archers").sumOf{m.optLong(it)})} جندي",style=MaterialTheme.typography.bodySmall)
            if(!hunt&&status in listOf("outbound","gathering")) TextButton({ask(Command("استدعاء المسيرة","/world/v3/gather/recall",json("march_id" to m.getLong("id"))))},enabled=vm.canAct){Text("استدعاء القوات")}
        }
    }
    selection?.let { target ->
        ArmyDispatch(vm,target,selectionType,onDismiss={selection=null}) { command->selection=null;ask(command) }
    }
}
@Composable
private fun TargetCard(name: String,level: Int,x:Int,y:Int,summary:String,enabled:Boolean,onSelect:()->Unit) { Panel {
    Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.Terrain,null,tint=Mint,modifier=Modifier.size(38.dp));Column(Modifier.padding(start=12.dp)){Text(name,fontWeight=FontWeight.Bold);Text("المستوى $level • $x، $y",color=Gold)}}
    Text(summary,style=MaterialTheme.typography.bodySmall)
    ActionButton("تجهيز الحملة",enabled,onSelect)
} }
@Composable
private fun ArmyDispatch(vm: FrontierViewModel,target:JSONObject,type:String,onDismiss:()->Unit,submit:(Command)->Unit) {
    var inf by remember {mutableStateOf("0")};var cav by remember {mutableStateOf("0")};var arc by remember {mutableStateOf("0")}
    val army=vm.me.obj("army");val i=inf.toLongOrNull()?:0;val c=cav.toLongOrNull()?:0;val a=arc.toLongOrNull()?:0
    val valid=i+c+a>0 && i<=army.optLong("infantry")&&c<=army.optLong("cavalry")&&a<=army.optLong("archers")
    AlertDialog(onDismissRequest=onDismiss,title={Text("تشكيل الحملة")},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){
        Text("حدد قواتك من الجيش المتاح. القوات المرسلة لن تكون متاحة لحملة أخرى حتى عودتها.")
        listOf(Triple("infantry",inf,{v:String->inf=v}),Triple("cavalry",cav,{v:String->cav=v}),Triple("archers",arc,{v:String->arc=v})).forEach{(key,value,set)->
            OutlinedTextField(value,{set(it.filter(Char::isDigit).take(7))},label={Text("${title(key)} • متاح ${num(army.optLong(key))}")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),singleLine=true)
        }
        TextButton({inf=army.optLong("infantry").toString();cav=army.optLong("cavalry").toString();arc=army.optLong("archers").toString()}){Text("اختيار جميع القوات")}
        if(type!="gather") Notice("قوة الهدف ${num(target.optLong("power"))}. الانتصار غير مضمون؛ قد تخسر قوات.",true)
        Text("إجمالي القوات: ${num(i+c+a)}",color=Gold)
        if(type=="classicHunt") Text("القوة: ${num(i*100+c*110+a*105)}",color=Mint)
    }},confirmButton={TextButton({
        val body=json("infantry" to i,"cavalry" to c,"archers" to a)
        val path=when(type){"gather"->{body.put("node_id",target.getLong("id"));"/world/v3/gather/start"};"classicHunt"->{body.put("monster_id",target.getLong("id"));"/world/monsters/hunt"};else->{body.put("monster_id",target.getLong("id"));"/world/v4/hunt/start"}}
        submit(Command(if(type=="gather") "إرسال حملة جمع" else "إرسال حملة صيد",path,body,if(type=="classicHunt") "hunt" else ""))
    },enabled=vm.canAct&&valid){Text("مراجعة الإرسال")}},dismissButton={TextButton(onDismiss){Text("إلغاء")}})
}

@Composable
private fun Journal(vm: FrontierViewModel, ask: (Command)->Unit, detail:(String,String)->Unit) {
    Section("المهام اليومية","استلم المكافآت بعد تحقيق الأهداف. التقدم والمكافآت يحددهما السيرفر.")
    vm.doc("/daily/quests").rows("quests").forEach {q->Panel {
        Text(title(q.optString("key")),fontWeight=FontWeight.Bold)
        Text("${q.optLong("progress")} / ${q.optLong("target")}",color=Mint)
        val reward=q.obj("rewards")
        Text(listOf("food","wood","stone","gold").filter{reward.optLong(it)>0}.joinToString(" · "){"${num(reward.optLong(it))} ${title(it)}"},style=MaterialTheme.typography.bodySmall)
        ActionButton(if(q.optBoolean("claimed")) "تم الاستلام" else "استلام المكافأة",vm.canAct&&q.optBoolean("complete")&&!q.optBoolean("claimed")){ask(Command("مكافأة ${title(q.optString("key"))}","/daily/quests/claim",json("key" to q.getString("key"))))}
    } }
    vm.doc("/progress/v2/daily").rows("missions").forEach {q->Panel {
        Text(title(q.optString("key")),fontWeight=FontWeight.Bold);Text("حدود المملكة • ${q.optLong("progress")} / ${q.optLong("target")}",color=Mint)
        ActionButton(if(q.optBoolean("claimed")) "تم الاستلام" else "استلام مكافأة الحدود",vm.canAct&&q.optBoolean("completed")&&!q.optBoolean("claimed")){ask(Command("مكافأة ${title(q.optString("key"))}","/progress/v2/daily/claim",json("key" to q.getString("key"))))}
    } }
    Section("تقارير الحملات")
    val hunts=vm.doc("/world/v4/hunt/reports").rows("reports")
    val gathers=vm.doc("/world/v3/gather/reports").rows("reports")
    if(hunts.isEmpty()&&gathers.isEmpty()) Notice("لا توجد تقارير حدود بعد. نتائج حملات المنطقة الأساسية تصلك بالبريد أدناه.")
    hunts.forEach {r->Panel {
        Text(if(r.optBoolean("monster_defeated")) "انتصار • هُزم الوحش" else "انتهى الاشتباك • الوحش ما زال حيًا",color=Gold,fontWeight=FontWeight.Bold)
        Text("ضرر ${num(r.optLong("damage_dealt"))} • مستوى الهدف ${r.optInt("monster_level")}")
        val rewards=r.obj("rewards")
        Text(listOf("food","wood","stone","gold").joinToString(" · "){"${num(rewards.optLong(it))} ${title(it)}"},style=MaterialTheme.typography.bodySmall)
        TextButton({detail("تفاصيل الخسائر",r.obj("attacker_losses").toString(2))}) {Text("الخسائر")}
    } }
    gathers.forEach {r->Panel {Text("اكتمل جمع ${title(r.optString("node_type"))}",color=Mint,fontWeight=FontWeight.Bold);Text("الغنيمة: ${num(r.optLong("total_amount"))}")} }
    Section("البريد")
    val mail=vm.doc("/mail").rows("mail")
    if(mail.isEmpty()) Notice("صندوق البريد فارغ")
    mail.take(30).forEach {m->Panel {
        Text(m.optString("subject"),fontWeight=FontWeight.Bold)
        Text(m.optString("body"),style=MaterialTheme.typography.bodySmall)
        if(!m.optBoolean("read")) TextButton({ask(Command("تحديد الرسالة كمقروءة","/mail/read",json("id" to m.getLong("id"))))},enabled=vm.canAct){Text("تمت القراءة")}
    } }
}
@Composable
private fun More(vm: FrontierViewModel,ask:(Command)->Unit) {
    var allianceName by rememberSaveable {mutableStateOf("")};var tag by rememberSaveable {mutableStateOf("")};var allianceId by rememberSaveable {mutableStateOf("")}
    var logout by remember {mutableStateOf(false)}
    Section("التحالف")
    val alliance=vm.doc("/alliances/me")
    Panel {
        if(alliance.has("id")) {
            Text("[${alliance.optString("tag")}] ${alliance.optString("name")}",color=Gold,fontSize=22.sp,fontWeight=FontWeight.Bold)
            Text("${alliance.optInt("members")} عضو • رقم التحالف ${alliance.optLong("id")}")
            ActionButton("تبرع 100 نقطة • 1,000 غذاء وخشب",vm.canAct&&vm.me.obj("resources").optLong("wood")>=1000&&vm.me.obj("resources").optLong("food")>=1000){ask(Command("تبرع للتحالف","/alliances/donate",json("tech_key" to "military","amount" to 100)))}
        } else {
            Text("أسّس تحالفك أو انضم برقم تحالف تعرفه.")
            OutlinedTextField(allianceName,{allianceName=it.take(32)},Modifier.fillMaxWidth(),label={Text("اسم التحالف")},singleLine=true)
            OutlinedTextField(tag,{tag=it.take(5)},Modifier.fillMaxWidth(),label={Text("اختصار التحالف")},singleLine=true)
            ActionButton("تأسيس التحالف",vm.canAct&&allianceName.length>=3&&tag.length>=2){ask(Command("تأسيس $allianceName","/alliances/create",json("name" to allianceName,"tag" to tag)))}
            HorizontalDivider()
            OutlinedTextField(allianceId,{allianceId=it.filter(Char::isDigit).take(10)},Modifier.fillMaxWidth(),label={Text("رقم التحالف")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),singleLine=true)
            ActionButton("انضمام",vm.canAct&&(allianceId.toLongOrNull()?:0)>0){ask(Command("الانضمام للتحالف $allianceId","/alliances/join",json("alliance_id" to allianceId.toLong())))}
        }
    }
    Section("الحقيبة")
    val items=vm.doc("/inventory").rows("items")
    if(items.isEmpty()) Notice("حقيبتك فارغة. تظهر العناصر المكتسبة هنا.")
    items.forEach {item->Panel {
        val key=item.optString("item_key",item.optString("key"))
        Text(item.optString("name",title(key)),fontWeight=FontWeight.Bold)
        Text("الكمية ${num(item.optLong("quantity"))}")
        ActionButton("استخدام عنصر واحد",vm.canAct&&item.optLong("quantity")>0){ask(Command("استخدام ${title(key)}","/inventory/use",json("item_key" to key,"count" to 1)))}
    } }
    Section("لوحة القوة")
    Panel { vm.doc("/leaderboards/power").rows("ranking").take(10).forEach {r->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("${r.optInt("rank")}. ${r.optString("username")}",Modifier.weight(1f));Text(num(r.optLong("power")),color=Gold)}} }
    Section("الإعدادات")
    Panel {
        Row(verticalAlignment=Alignment.CenterVertically){Text("تقليل حركة الخلفية",Modifier.weight(1f));Switch(vm.reduceMotion,{vm.setMotion(it)})}
        Text("حُدود المملكة • ${BuildConfig.VERSION_NAME} بيتا",color=Gold)
        Text("المدينة مرتبطة بحسابك. احتفظ باسم الحساب وكلمة المرور.",style=MaterialTheme.typography.bodySmall)
        TextButton({logout=true},enabled=!vm.busy&&!vm.refreshing){Text("تسجيل الخروج")}
    }
    if(logout) AlertDialog(onDismissRequest={logout=false},title={Text("تسجيل الخروج؟")},text={Text("تبقى مدينتك على السيرفر. ستحتاج إلى بيانات حسابك للعودة.")},confirmButton={TextButton({logout=false;vm.logout()}){Text("خروج")}},dismissButton={TextButton({logout=false}){Text("إلغاء")}})
}
