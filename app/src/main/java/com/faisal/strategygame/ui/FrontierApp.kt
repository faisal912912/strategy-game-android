package com.faisal.strategygame.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.shape.CircleShape
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
    "deadly_focus" to "تركيز قاتل", "wall_of_iron" to "جدار الحديد", "royal_command" to "الأمر الملكي", "thunder_charge" to "اندفاع الرعد",
    "guardian_armor" to "درع الحارس", "scout_boots" to "حذاء الكشّاف", "iron_sword" to "السيف الحديدي",
    "food_pack_10k" to "صندوق 10 آلاف غذاء", "wood_pack_10k" to "صندوق 10 آلاف خشب", "stone_pack_5k" to "صندوق 5 آلاف حجر",
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
fun countdown(end: Long, now: Long): String { val s=((end-now+999)/1000).coerceAtLeast(0); return when {s==0L->"جاهز";s>=86400->"${s/86400} ي • %02d:%02d".format(s%86400/3600,s%3600/60);s>=3600->"%02d:%02d:%02d".format(s/3600,s%3600/60,s%60);else->"%02d:%02d".format(s/60,s%60)} }

@Composable
fun FrontierApp(vm: FrontierViewModel = viewModel()) {
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(vm, owner) { owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        var ticks=0
        while(true) { vm.tick(); if(ticks++%20==0) vm.refresh(); delay(1000) }
    } }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Surface(color=Ink,contentColor=Parchment) { if (!vm.signedIn) Gate(vm) else Kingdom(vm) }
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
        ArtworkBanner(com.faisal.strategygame.R.drawable.town_board,"مملكتك تبدأ هنا","مدينة تنتظر حاكمها")
        Column(Modifier.padding(24.dp), verticalArrangement=Arrangement.spacedBy(14.dp)) {
            Text("حُدود المملكة", fontSize=34.sp, fontWeight=FontWeight.Black, color=Gold)
            Text("ابنِ مدينتك. جهّز جيشك. اكتشف العالم.", color=Mint)
            Text("المملكة الأولى • بيتا 0.5", color=Parchment, style=MaterialTheme.typography.labelLarge)
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
internal fun Kingdom(vm: FrontierViewModel) {
    var confirmation by remember { mutableStateOf<Command?>(null) }
    var detail by remember { mutableStateOf<Pair<String,String>?>(null) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(vm.message) { vm.message?.let { snackbar.showSnackbar(it); vm.clearMessage() } }
    val ask: (Command)->Unit = { confirmation=it }
    var showJobs by remember { mutableStateOf(false) }
    var showResources by remember { mutableStateOf(false) }
    var showProfile by remember { mutableStateOf(false) }
    val scene=vm.tab in listOf("city","world")
    val screenStates=rememberSaveableStateHolder()
    val keyboard=LocalSoftwareKeyboardController.current
    LaunchedEffect(vm.tab) {keyboard?.hide()}
    BackHandler(vm.tab!="city") { vm.selectTab("city") }
    val hud: @Composable ()->Unit = {
        Column {
            RealmHud(vm.me.optString("username"),vm.me.optLong("power"),vm.me.obj("resources"),
                vm.me.obj("city").optInt("level",1),vm.busy||vm.refreshing,{showProfile=true},{showResources=true},{vm.refresh()})
            vm.error?.let { Notice(it,true) }
            vm.pending?.let { c -> Row(Modifier.fillMaxWidth().background(Slate).padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically) {
                Text("بانتظار تأكيد: ${c.title}",Modifier.weight(1f),fontSize=12.sp,color=Gold)
                TextButton({vm.retryPending()},enabled=!vm.busy&&!vm.refreshing){Text("تحقق")}
            } }
        }
    }
    Scaffold(modifier=Modifier.imePadding(),containerColor=Ink,contentWindowInsets=WindowInsets(0,0,0,0),snackbarHost={SnackbarHost(snackbar)},
        topBar={if(!scene)hud()},bottomBar={Column {ActivityStrip(vm){showJobs=true};RoyalDock(vm.tab,vm.reduceMotion){vm.selectTab(it)}}}) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            val currentTab=vm.tab
            screenStates.SaveableStateProvider(currentTab) {
                when(currentTab) {
                    "city" -> TownScreen(vm,ask)
                    "world" -> KingdomMap(vm,ask)
                    // A screen is one column: keep its forms directly under the screen registry.
                    // Nesting a single lazy item here discarded its saveable form state on tab changes.
                    else -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start=16.dp,end=16.dp,bottom=24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                        when(currentTab) {
                            "army" -> MilitaryScreen(vm,ask)
                            "heroes" -> HeroesScreen(vm,ask)
                            "shop" -> ShopScreen(vm,ask)
                            "warehouse" -> WarehouseScreen(vm,ask)
                            "missions" -> {RoyalHeading("سجل المملكة","المهام والمكافآت");Journal(vm,ask){a,b->detail=a to b}}
                            else -> {RoyalHeading("مجلس الحاكم","ديوان المملكة","كل ما تحتاجه لإدارة مملكتك");More(vm,ask)}
                        }
                    }
                }
            }
            if(scene)Box(Modifier.align(Alignment.TopCenter)){hud()}
        }
    }
    if(showJobs) RoyalSheet("طابور أعمال المملكة",{showJobs=false}) {Jobs(vm){showJobs=false;ask(it)};ActionButton("المستودع والتسريعات"){showJobs=false;vm.selectTab("warehouse")}}
    if(showProfile) RoyalSheet("ملف الحاكم",{showProfile=false}) {
        GovernorCard(vm)
        Panel {
            Text("القوات المتاحة",color=Gold,fontWeight=FontWeight.Bold)
            listOf("infantry","cavalry","archers").forEach {key->
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {Text(title(key));Text(num(vm.me.obj("army").optLong(key)),color=Mint)}
            }
        }
        ActionButton("فتح ديوان المملكة"){showProfile=false;vm.selectTab("more")}
    }
    if(showResources) RoyalSheet("مخزون المدينة",{showResources=false}) {
        Text("رصيد الموارد المتاح للبناء والتدريب",color=Mint,fontSize=12.sp)
        listOf("food","wood","stone","gold").forEach{k->Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Ink).padding(14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            Icon(resourceIcon(k),null,tint=Gold);Text(title(k),Modifier.weight(1f));Text(num(vm.me.obj("resources").optLong(k)),color=Mint,fontWeight=FontWeight.Bold)
        }}
        ActionButton("البحث عن موارد في المملكة"){showResources=false;vm.selectTab("world")}
        ActionButton("فتح المستودع والتسريعات"){showResources=false;vm.selectTab("warehouse")}
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
private fun commandDescription(c: Command): String = if(c.details.isNotEmpty()) c.details else when {
    c.path.endsWith("/hunt/start") || c.path.endsWith("/monsters/hunt") -> "ستغادر القوات المحددة المدينة. يحسب السيرفر النتيجة والخسائر؛ قد تحتاج القوات المصابة إلى العلاج."
    c.path.endsWith("/recall") -> "تبدأ رحلة العودة. الاستدعاء يلغي حمولة الجمع الحالية."
    c.path=="/game/buildings/upgrade" -> "ستُخصم تكلفة الترقية الموضحة. يمكنك مواصلة اللعب أثناء البناء."
    else -> "تنفيذ ${c.title} بالاختيارات والتكلفة الموضحة؟"
}
@Composable
fun Panel(content: @Composable ColumnScope.()->Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Brush.linearGradient(listOf(Color(0xFF263C48),Slate))).border(1.dp,Bronze.copy(.45f),RoundedCornerShape(18.dp)).padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp),content=content)
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
    Button({haptics.performHapticFeedback(HapticFeedbackType.LongPress);action()},Modifier.fillMaxWidth().heightIn(min=50.dp).scale(scale).shadow(if(enabled) 4.dp else 0.dp,RoundedCornerShape(10.dp)).background(Brush.verticalGradient(if(enabled) listOf(Color(0xFFF2D58C),Color(0xFFBB8D41)) else listOf(Slate,Slate)),RoundedCornerShape(10.dp)).border(1.dp,if(enabled) Gold else Bronze.copy(.25f),RoundedCornerShape(10.dp)),enabled=enabled,interactionSource=interaction,shape=RoundedCornerShape(10.dp),colors=ButtonDefaults.buttonColors(containerColor=Color.Transparent,contentColor=Ink,disabledContainerColor=Color.Transparent,disabledContentColor=Parchment.copy(.35f))) { Text(text,fontWeight=FontWeight.Bold) }
}
@Composable
private fun Section(heading: String, subtitle: String="") { Text(heading,fontSize=23.sp,fontWeight=FontWeight.Bold,color=Gold);if(subtitle.isNotEmpty()) Text(subtitle,color=Color(0xFF9FB3C2),style=MaterialTheme.typography.bodySmall) }
@Composable
private fun Jobs(vm: FrontierViewModel, ask: (Command)->Unit) { Panel {
    Text("أعمال المدينة",color=Gold,fontWeight=FontWeight.Bold)
    vm.jobs.forEach { job ->
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(jobName(job),Modifier.weight(1f));Text(countdown(job.ends,vm.now),color=Mint)}
        if(vm.now>=job.ends) {
            val path=job.claimPath.ifEmpty { when(job.kind){"build"->"/game/buildings/claim";"train"->"/game/training/claim";else->"/game/research/claim"} }
            ActionButton("استلام ${job.label}",vm.canAct){ask(Command("استلام ${job.label}",path,if(job.claimId>0) json("id" to job.claimId) else JSONObject()))}
        }
    }
} }
@Composable
fun ArmySupport(vm: FrontierViewModel, ask: (Command)->Unit) {
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
}

@Composable
fun World(vm: FrontierViewModel, ask: (Command)->Unit) {
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
fun ArmyDispatch(vm: FrontierViewModel,target:JSONObject,type:String,onDismiss:()->Unit,submit:(Command)->Unit) {
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
    var page by rememberSaveable {mutableStateOf("home")}
    BackHandler(page!="home") {page="home"}
    if(page=="home") {
        GovernorCard(vm)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            CouncilTile("التحالف","الأعضاء والتبرعات",Icons.Default.Groups,Modifier.weight(1f)){page="alliance"}
            CouncilTile("المستودع","التسريعات والموارد والأغراض",Icons.Default.Inventory2,Modifier.weight(1f)){vm.selectTab("warehouse")}
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            CouncilTile("المهام والبريد","السجل ومكافآت المملكة",Icons.Default.Assignment,Modifier.weight(1f)){vm.selectTab("missions")}
            CouncilTile("لوحة القوة","ترتيب حكّام المملكة",Icons.Default.EmojiEvents,Modifier.weight(1f)){page="ranking"}
        }
        CouncilTile("الإعدادات","الحركة والحساب",Icons.Default.Settings,Modifier.fillMaxWidth()){page="settings"}
    } else TextButton({page="home"}) {Icon(Icons.Default.ArrowForward,null);Spacer(Modifier.width(6.dp));Text("العودة إلى الديوان")}
    if(page=="alliance") {
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
    }
    if(page=="ranking") {
    Section("لوحة القوة")
    if(vm.doc("/leaderboards/power").rows("ranking").isEmpty()) EmptyState("لا يوجد ترتيب بعد","تظهر قائمة الحكّام عندما تتوفر بيانات المملكة.",Icons.Default.EmojiEvents)
    Panel { vm.doc("/leaderboards/power").rows("ranking").take(10).forEach {r->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("${r.optInt("rank")}. ${r.optString("username")}",Modifier.weight(1f));Text(num(r.optLong("power")),color=Gold)}} }
    }
    if(page=="settings") {
    Section("الإعدادات")
    Panel {
        Row(verticalAlignment=Alignment.CenterVertically){Text("تقليل حركة الخلفية",Modifier.weight(1f));Switch(vm.reduceMotion,{vm.setMotion(it)})}
        Text("حُدود المملكة • ${BuildConfig.VERSION_NAME} بيتا",color=Gold)
        Text("المدينة مرتبطة بحسابك. احتفظ باسم الحساب وكلمة المرور.",style=MaterialTheme.typography.bodySmall)
        TextButton({logout=true},enabled=!vm.busy&&!vm.refreshing){Text("تسجيل الخروج")}
    }
    }
    if(logout) AlertDialog(onDismissRequest={logout=false},title={Text("تسجيل الخروج؟")},text={Text("تبقى مدينتك على السيرفر. ستحتاج إلى بيانات حسابك للعودة.")},confirmButton={TextButton({logout=false;vm.logout()}){Text("خروج")}},dismissButton={TextButton({logout=false}){Text("إلغاء")}})
}
