package com.faisal.strategygame.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import com.faisal.strategygame.*
import com.faisal.strategygame.R
import com.faisal.strategygame.data.*
import kotlinx.coroutines.delay
import org.json.JSONObject
import kotlin.math.*

data class BuildingSpot(val key:String,val x:Float,val y:Float)
val townSpots=listOf(BuildingSpot("castle",500f,390f),BuildingSpot("barracks",215f,355f),BuildingSpot("archery_range",795f,350f),BuildingSpot("stable",790f,523f),BuildingSpot("hospital",236f,520f),BuildingSpot("academy",500f,595f),BuildingSpot("warehouse",500f,780f),BuildingSpot("farm",210f,717f),BuildingSpot("lumber_mill",260f,190f),BuildingSpot("quarry",718f,207f),BuildingSpot("gold_mine",808f,735f))

@Composable
fun TownScreen(vm:FrontierViewModel,ask:(Command)->Unit) {
    var selected by rememberSaveable {mutableStateOf<String?>(null)}
    var ledger by rememberSaveable {mutableStateOf(false)}
    val buildings=vm.doc("/game/buildings").rows("buildings")
    Box(Modifier.fillMaxSize()) {
        TownBoard(buildings.associate{it.optString("type") to it.optInt("level")},vm.jobs.firstOrNull{it.kind=="build"}?.let {"${it.label} • ${countdown(it.ends,vm.now)}"},vm.reduceMotion,{selected=it})
        Column(Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top=120.dp,end=8.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
            GameMedallion(Icons.Default.AccountBalance,"المباني"){ledger=true}
            GameMedallion(Icons.Default.Assignment,"المهام"){vm.selectTab("missions")}
            GameMedallion(Icons.Default.Redeem,"العروض"){vm.selectTab("shop")}
        }
        val castle=buildings.firstOrNull{it.optString("type")=="castle"}?.optInt("level") ?: 1
        Box(Modifier.align(Alignment.BottomCenter).padding(10.dp)) {
            QuestRibbon("تطوير المملكة",if(castle<30) "طوّر القلعة إلى المستوى ${castle+1}" else "أكمل أعمال المدينة واستلم مكافآتك",castle/30f) {if(castle<30)selected="castle" else vm.selectTab("missions")}
        }

    }
    selected?.let { key -> val b=buildings.firstOrNull{it.optString("type")==key}
        AlertDialog(onDismissRequest={selected=null},title={Text(title(key))},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            BuildingPortrait(key,Modifier.fillMaxWidth().height(165.dp))
            if(b!=null) BuildingDetails(vm,b) { selected=null;ask(it) } else Text("جارٍ تحميل مستوى المبنى…")
            when(key) {
                "barracks","stable","archery_range","academy","hospital"-> TextButton({selected=null;vm.selectTab("army")}) {Text("فتح التدريب والعلاج والأبحاث")}
                "warehouse"->TextButton({selected=null;vm.selectTab("more")}) {Text("فتح الحقيبة")}
            }
        }},confirmButton={TextButton({selected=null}){Text("إغلاق")}})
    }
    if(ledger) AlertDialog(onDismissRequest={ledger=false},title={Text("سجل المدينة")},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        val p=vm.doc("/city/progression")
        Text("خبرة المدينة ${num(p.optLong("xp"))} / ${num(p.optLong("xp_for_next_level"))}",color=Gold)
        if(p.optBoolean("can_upgrade")) ActionButton("ترقية مستوى المدينة",vm.canAct){ledger=false;ask(Command("ترقية المدينة","/city/progression/upgrade",JSONObject()))}
        buildings.forEach{b->TextButton({ledger=false;selected=b.optString("type")},Modifier.fillMaxWidth()){Text("${title(b.optString("type"))} • المستوى ${b.optInt("level")}")}}
        ActionButton("استلام بناء مكتمل",vm.canAct){ledger=false;ask(Command("استلام البناء المكتمل","/game/buildings/claim",JSONObject()))}
    }},confirmButton={TextButton({ledger=false}){Text("إغلاق")}})
}

@Composable
fun TownBoard(levels:Map<String,Int>,buildLabel:String?,reduced:Boolean,onBuilding:(String)->Unit) {
    var camera by remember {mutableStateOf(MapCamera(500f,470f,1.35f))}
    val board=ImageBitmap.imageResource(R.drawable.town_board)
    val phase=scenePhase(reduced)
    val buildingClick by rememberUpdatedState(onBuilding)
    BoxWithConstraints(Modifier.fillMaxSize().background(Color(0xFF253D31)).clipToBoundsCompat().testTag("town-board").pointerInput(Unit) {detectTransformGestures {centroid,pan,zoom,_->
            val w=size.width.toFloat();val h=size.height.toFloat();val base=w/1000f
            val z=camera.zoomAt(zoom,centroid.x-w/2,centroid.y-h/2,base,1000f)
            camera=z.pan(pan.x,pan.y,base*z.zoom,1000f)
        }}.pointerInput(Unit) {detectTapGestures(onDoubleTap={camera=camera.copy(zoom=if(camera.zoom>2f)1.35f else 2.4f)},onTap={tap->
            val scale=size.width/1000f*camera.zoom
            val world=Offset(camera.x+(tap.x-size.width/2)/scale,camera.y+(tap.y-size.height/2)/scale)
            townSpots.minByOrNull {hypot(it.x-world.x,it.y-45-world.y)}?.let {spot->if(hypot(spot.x-world.x,spot.y-45-world.y)<105f)buildingClick(spot.key)}
        })}) {
        val density=LocalDensity.current;val w=constraints.maxWidth.toFloat();val h=constraints.maxHeight.toFloat()
        val base=w/1000f;val scale=base*camera.zoom
        fun pos(x:Float,y:Float)=Offset(w/2+(x-camera.x)*scale,h/2+(y-camera.y)*scale)
        Canvas(Modifier.fillMaxSize()) {
            val origin=pos(0f,0f);val side=(1000*scale).roundToInt()
            drawImage(board,dstOffset=IntOffset(origin.x.roundToInt(),origin.y.roundToInt()),dstSize=IntSize(side,side),filterQuality=FilterQuality.Medium)
            // Decorative atmosphere stays separate from authoritative game state.
            listOf(267f to 156f,804f to 488f).forEach{(x,y)->repeat(4){i->
                val t=(phase*5+i*.25f)%1f
                drawCircle(Color(0xFFE7E1CB).copy((1-t)*.23f),(5+t*12)*scale,pos(x+sin(t*6)*9,y-t*66))
            }}
            if(buildLabel!=null) {
                val spot=townSpots.firstOrNull{buildLabel.contains(title(it.key))} ?: townSpots.first()
                drawOval(Gold.copy(.6f),pos(spot.x-60,spot.y-10),Size(120*scale,32*scale),style=Stroke(2f*scale))
            }
            // Small citizens follow the courtyard roads; motion stops with Reduce motion.
            repeat(8) {i->val t=(phase+i*.125f)%1f;val p=pos(380f+t*250,445f+sin(t*6.283f+i)*85)
                drawOval(Color.Black.copy(.23f),p+Offset(-3f,3f),androidx.compose.ui.geometry.Size(8f,4f))
                drawCircle(if(i%2==0) Color(0xFF3068AA) else Gold,3f*scale,p)
                drawCircle(Color(0xFFE1B494),1.8f*scale,p-Offset(0f,3.3f*scale))
            }
            listOf(445f to 411f,554f to 410f,380f to 713f,640f to 727f).forEachIndexed {i,(x,y)->
                val p=pos(x,y);drawCircle(Gold.copy(.1f+.05f*sin(phase*40+i)),12f*scale,p);drawCircle(Color(0xFFFFD17C),2.3f*scale,p)
            }
        }
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            townSpots.forEach {spot->val p=pos(spot.x,spot.y)
                if(p.x in -90f..w+90&&p.y in -100f..h+100) {
                    Column(Modifier.absoluteOffset{IntOffset((p.x-with(density){45.dp.toPx()}).roundToInt(),(p.y-with(density){38.dp.toPx()}).roundToInt())}.width(90.dp).heightIn(min=66.dp).clip(RoundedCornerShape(12.dp)).clickable {onBuilding(spot.key)}.testTag("building-${spot.key}"),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Bottom) {
                        Spacer(Modifier.height(28.dp))
                        Text(title(spot.key),Modifier.background(Ink.copy(.88f),RoundedCornerShape(topStart=6.dp,topEnd=6.dp)).padding(horizontal=8.dp,vertical=3.dp),fontSize=10.sp,color=Parchment,fontWeight=FontWeight.Bold,maxLines=1)
                        Text(levels[spot.key]?.let{"Lv. $it"}?:"…",Modifier.background(Emerald.copy(.96f),RoundedCornerShape(bottomStart=6.dp,bottomEnd=6.dp)).padding(horizontal=9.dp,vertical=2.dp),fontSize=10.sp,color=Gold)
                    }
                }
            }
        }
        Column(Modifier.align(Alignment.BottomEnd).padding(end=10.dp,bottom=94.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            SceneButton(Icons.Default.Add,"تكبير المدينة") {camera=camera.copy(zoom=(camera.zoom*1.25f).coerceAtMost(4f))}
            SceneButton(Icons.Default.Remove,"تصغير المدينة") {camera=camera.copy(zoom=(camera.zoom/1.25f).coerceAtLeast(.5f))}
            SceneButton(Icons.Default.CenterFocusStrong,"مركز المدينة") {camera=MapCamera(500f,470f,1.35f)}
        }
        buildLabel?.let {Text(it,Modifier.align(Alignment.BottomStart).padding(start=10.dp,bottom=90.dp).background(Ink.copy(.9f),RoundedCornerShape(12.dp)).padding(10.dp),fontSize=12.sp,color=Gold)}
    }
}
private fun Modifier.clipToBoundsCompat()=this.then(Modifier.clip(RoundedCornerShape(0.dp)))
@Composable private fun scenePhase(reduced:Boolean):Float {
    if(reduced) return .2f
    val transition=rememberInfiniteTransition(label="living scene")
    val value by transition.animateFloat(0f,1f,infiniteRepeatable(tween(24000,easing=LinearEasing)),label="citizens")
    return value
}
@Composable private fun SceneButton(icon:androidx.compose.ui.graphics.vector.ImageVector,label:String,action:()->Unit) {
    FilledIconButton(action,colors=IconButtonDefaults.filledIconButtonColors(containerColor=Ink.copy(.92f),contentColor=Gold),modifier=Modifier.size(44.dp)) {Icon(icon,label)}
}

@Composable
fun BuildingPortrait(key:String,modifier:Modifier=Modifier) {
    val board=ImageBitmap.imageResource(R.drawable.town_board)
    val spot=townSpots.firstOrNull{it.key==key} ?: townSpots.first()
    Canvas(modifier.clip(RoundedCornerShape(14.dp))) {
        val region=if(key=="castle") 330f else 245f
        val x=(spot.x-region/2).coerceIn(0f,1000f-region)
        val y=(spot.y-region*.72f).coerceIn(0f,1000f-region)
        drawImage(board,srcOffset=IntOffset((x*board.width/1000).toInt(),(y*board.height/1000).toInt()),srcSize=IntSize((region*board.width/1000).toInt(),(region*board.height/1000).toInt()),dstSize=IntSize(size.width.toInt(),size.height.toInt()),filterQuality=FilterQuality.High)
        drawRect(Brush.verticalGradient(listOf(Color.Transparent,Ink.copy(.6f))))
    }
}

@Composable
private fun BuildingDetails(vm:FrontierViewModel,b:JSONObject,ask:(Command)->Unit) {
    val key=b.optString("type");val level=b.optInt("level");val next=level+1;val res=vm.me.obj("resources")
    Text("المستوى $level / 30",fontSize=22.sp,fontWeight=FontWeight.Bold,color=Gold)
    if(level>=30) {Text("وصل المبنى إلى أعلى مستوى.",color=Mint);return}
    val cost="${num(next*700L)} غذاء · ${num(next*700L)} خشب\n${num(next*350L)} حجر · ${num(next*100L)} ذهب"
    Text("متطلبات المستوى $next",fontWeight=FontWeight.Bold)
    ResourceCosts(mapOf("food" to next*700L,"wood" to next*700L,"stone" to next*350L,"gold" to next*100L),res)
    Text("المدة: ${countdown(next*30000L,0)}",color=Mint)
    val enough=res.optLong("food")>=next*700L&&res.optLong("wood")>=next*700L&&res.optLong("stone")>=next*350L&&res.optLong("gold")>=next*100L
    val queue=vm.jobs.any{it.kind=="build"}
    ActionButton(if(queue) "البنّاء مشغول" else if(!enough) "الموارد غير كافية" else "ترقية إلى المستوى $next",vm.canAct&&enough&&!queue) {ask(Command("ترقية ${title(key)}","/game/buildings/upgrade",json("building" to key),"build",details="$cost\nالمدة: ${countdown(next*30000L,0)}"))}
}

data class WorldPin(val key:String,val kind:String,val x:Float,val y:Float,val name:String,val level:Int,val data:JSONObject)
@Composable
fun KingdomMap(vm:FrontierViewModel,ask:(Command)->Unit) {
    var selected by remember {mutableStateOf<WorldPin?>(null)};var dispatch by remember {mutableStateOf<WorldPin?>(null)}
    var filter by rememberSaveable {mutableStateOf("all")}
    var explorer by rememberSaveable {mutableStateOf(false)}
    var coordinates by rememberSaveable {mutableStateOf(false)};var campaigns by rememberSaveable {mutableStateOf(false)}
    var x by rememberSaveable {mutableStateOf("")};var y by rememberSaveable {mutableStateOf("")}
    var camera by remember {mutableStateOf(MapCamera(vm.scanX.toFloat(),vm.scanY.toFloat()))}
    var located by remember {mutableStateOf(false)}
    val location=vm.doc("/world/v2/state")
    LaunchedEffect(location.toString()) {if(!located&&location.has("x")){camera=MapCamera(location.getInt("x").toFloat(),location.getInt("y").toFloat());located=true;vm.scan(camera.x.roundToInt(),camera.y.roundToInt())}}
    LaunchedEffect(camera.x.roundToInt(),camera.y.roundToInt()) {delay(650);vm.scan(camera.x.roundToInt(),camera.y.roundToInt())}
    val nodes=vm.doc(vm.nodePath).rows("nodes");val monsters=vm.doc(vm.monsterPath).rows("monsters")
    val pins=vm.doc(vm.cityPath).rows("cities").map {WorldPin("city-${it.optLong("player_id")}","city",it.optInt("x").toFloat(),it.optInt("y").toFloat(),it.optString("username"),0,it)}+
        nodes.filter{it.optString("status")=="active"}.map{WorldPin("node-${it.optLong("id")}","gather",it.optInt("x").toFloat(),it.optInt("y").toFloat(),title(it.optString("node_type")),it.optInt("level"),it)}+
        monsters.filter{it.optString("status")=="active"}.map{WorldPin("monster-${it.optLong("id")}","hunt",it.optInt("x").toFloat(),it.optInt("y").toFloat(),it.optString("name"),it.optInt("level"),it)}
    Box(Modifier.fillMaxSize()) {
        WorldBoard(camera,{camera=it},pins.filter{filter=="all"||it.kind==filter},vm.reduceMotion){selected=it}
        Column(Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top=155.dp,start=12.dp,end=12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
            ChoiceTabs(listOf("all" to "الكل","city" to "المدن","gather" to "الموارد","hunt" to "الوحوش"),filter){filter=it}
        }
        Box(Modifier.align(Alignment.BottomStart).padding(start=12.dp,bottom=80.dp)) {
            KingdomMinimap(camera,pins){camera=MapCamera(it.x,it.y,camera.zoom)}
        }
        Column(Modifier.align(Alignment.CenterEnd).padding(10.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            SceneButton(Icons.Default.Add,"تكبير الخريطة"){camera=camera.copy(zoom=(camera.zoom*1.3f).coerceAtMost(4f))}
            SceneButton(Icons.Default.Remove,"تصغير الخريطة"){camera=camera.copy(zoom=(camera.zoom/1.3f).coerceAtLeast(.5f))}
            SceneButton(Icons.Default.Home,"موقع مدينتي"){if(location.has("x"))camera=MapCamera(location.optInt("x").toFloat(),location.optInt("y").toFloat())}
            SceneButton(Icons.Default.Search,"بحث بالإحداثيات"){x=camera.x.roundToInt().toString();y=camera.y.roundToInt().toString();coordinates=true}
            SceneButton(Icons.Default.Flag,"الحملات والمسيرات"){campaigns=true}
            SceneButton(Icons.Default.TravelExplore,"الأهداف القريبة"){explorer=true}
        }
        Text("#1  •  X:${camera.x.roundToInt()}   Y:${camera.y.roundToInt()}  •  طاقة ${vm.doc("/world/v4/pve/status").optInt("energy")}",Modifier.align(Alignment.BottomCenter).padding(12.dp).background(Ink.copy(.9f),RoundedCornerShape(12.dp)).padding(10.dp),fontSize=11.sp,color=Gold)
    }
    selected?.let {pin->AlertDialog(onDismissRequest={selected=null},title={Text(pin.name)},text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Image(painterResource(pinArt(pin)),null,Modifier.fillMaxWidth().height(160.dp),contentScale=ContentScale.Fit)
        Text("X:${pin.x.toInt()} • Y:${pin.y.toInt()}",color=Gold)
        if(pin.level>0)Text("المستوى ${pin.level}")
        when(pin.kind) {
            "gather"->Text("موارد متبقية: ${num(pin.data.optLong("remaining_amount"))}",color=Mint)
            "hunt"->{Text("القوة ${num(pin.data.optLong("power"))}");Text("الصحة ${num(pin.data.optLong("current_hp"))} / ${num(pin.data.optLong("max_hp"))}")}
            else->Text(if(pin.data.optLong("player_id")==vm.me.optLong("player_id")) "هذه مدينتك" else "مدينة حاكم في المملكة")
        }
    }},confirmButton={if(pin.kind!="city")TextButton({dispatch=pin;selected=null},enabled=vm.canAct){Text("تجهيز الحملة")}else TextButton({selected=null;if(pin.data.optLong("player_id")==vm.me.optLong("player_id"))vm.selectTab("city")}){Text(if(pin.data.optLong("player_id")==vm.me.optLong("player_id")) "دخول المدينة" else "إغلاق")}},dismissButton={if(pin.kind!="city") TextButton({selected=null}){Text("إغلاق")}})}
    dispatch?.let{pin->ArmyDispatch(vm,pin.data,pin.kind,{dispatch=null}){dispatch=null;ask(it)}}
    if(coordinates)AlertDialog(onDismissRequest={coordinates=false},title={Text("انتقال على الخريطة")},text={Column {Text("أدخل إحداثيات من 0 إلى 499. يغيّر هذا موضع العرض فقط.");OutlinedTextField(x,{x=it.filter(Char::isDigit).take(3)},label={Text("X")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number));OutlinedTextField(y,{y=it.filter(Char::isDigit).take(3)},label={Text("Y")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number))}},confirmButton={TextButton({camera=MapCamera(x.toFloat(),y.toFloat(),camera.zoom);coordinates=false},enabled=(x.toIntOrNull()?:-1) in 0..499&&(y.toIntOrNull()?:-1) in 0..499){Text("استعراض")}},dismissButton={TextButton({coordinates=false}){Text("إلغاء")}})
    if(explorer)AlertDialog(onDismissRequest={explorer=false},title={Text("أهداف حول موقع العرض")},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        val nearby=pins.filter{filter=="all"||it.kind==filter}.sortedBy{hypot(it.x-camera.x,it.y-camera.y)}.take(30)
        if(nearby.isEmpty())Text("لا توجد أهداف في المسح الحالي. جرّب موضعًا آخر أو غيّر الفلتر.")
        nearby.forEach{pin->Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Slate).clickable{camera=MapCamera(pin.x,pin.y,camera.zoom);explorer=false;selected=pin}.padding(8.dp),verticalAlignment=Alignment.CenterVertically) {
            Image(painterResource(pinArt(pin)),null,Modifier.size(48.dp))
            Column(Modifier.weight(1f)) {Text(pin.name,color=Gold,fontWeight=FontWeight.Bold);Text("X:${pin.x.toInt()}  Y:${pin.y.toInt()} • ${hypot(pin.x-camera.x,pin.y-camera.y).roundToInt()} خانة",color=Mint,fontSize=11.sp)}
        }}
    }},confirmButton={TextButton({explorer=false}){Text("إغلاق")}})
    if(campaigns)AlertDialog(onDismissRequest={campaigns=false},title={Text("الحملات والمسيرات")},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)){World(vm){campaigns=false;ask(it)}}},confirmButton={TextButton({campaigns=false}){Text("إغلاق")}})
}

@Composable
fun WorldBoard(camera:MapCamera,onCamera:(MapCamera)->Unit,pins:List<WorldPin>,reduced:Boolean,onPin:(WorldPin)->Unit) {
    val texture=ImageBitmap.imageResource(R.drawable.world_terrain)
    val current by rememberUpdatedState(camera)
    val phase=scenePhase(reduced)
    BoxWithConstraints(Modifier.fillMaxSize().background(Color(0xFF355538)).clipToBoundsCompat().testTag("world-board").pointerInput(Unit) {detectTransformGestures {centroid,pan,zoom,_->
            val w=size.width.toFloat();val h=size.height.toFloat();val base=w/80f
            val z=current.zoomAt(zoom,centroid.x-w/2,centroid.y-h/2,base,499f)
            onCamera(z.pan(pan.x,pan.y,base*z.zoom,499f))
        }}) {
        val w=constraints.maxWidth.toFloat();val h=constraints.maxHeight.toFloat();val density=LocalDensity.current
        val base=w/80f;val scale=base*camera.zoom
        fun pos(x:Float,y:Float)=Offset(w/2+(x-camera.x)*scale,h/2+(y-camera.y)*scale)
        Canvas(Modifier.fillMaxSize()) {
            // Decorative terrain tiles are separate from authoritative entity coordinates.
            for(tx in 0..4)for(ty in 0..4) {val p=pos(tx*100f,ty*100f);val side=ceil(100*scale).toInt()
                if(p.x+side>=0&&p.y+side>=0&&p.x<=w&&p.y<=h)drawImage(texture,dstOffset=IntOffset(p.x.roundToInt(),p.y.roundToInt()),dstSize=IntSize(side+1,side+1),filterQuality=FilterQuality.Medium)
            }
            pins.filter{it.kind=="hunt"}.forEach{p->drawCircle(Color(0xFFFFAE62).copy(.12f),with(density){30.dp.toPx()}*(.9f+.1f*sin(phase*30)),pos(p.x,p.y))}
        }
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            pins.sortedBy{it.y}.forEach {pin->val p=pos(pin.x,pin.y);val width=if(pin.kind=="city") 98.dp else 80.dp
                if(p.x in -100f..w+100&&p.y in -100f..h+100) {
                    Column(Modifier.absoluteOffset{IntOffset((p.x-with(density){width.toPx()/2}).roundToInt(),(p.y-with(density){58.dp.toPx()}).roundToInt())}.width(width).clip(RoundedCornerShape(10.dp)).clickable{onPin(pin)}.testTag(pin.key),horizontalAlignment=Alignment.CenterHorizontally) {
                        Image(painterResource(pinArt(pin)),pin.name,Modifier.size(if(pin.kind=="city") 90.dp else 70.dp).graphicsLayer {if(pin.kind=="hunt"){scaleY=1f+.015f*sin(phase*38);transformOrigin=TransformOrigin(.5f,1f)}},contentScale=ContentScale.Fit)
                        Text((if(pin.level>0) "${pin.level} • " else "")+pin.name,Modifier.background(Ink.copy(.92f),RoundedCornerShape(5.dp)).padding(horizontal=6.dp,vertical=3.dp),color=if(pin.kind=="city") Mint else Color.White,fontSize=10.sp,fontWeight=FontWeight.Bold,maxLines=1)
                    }
                }
            }
        }
    }
}

fun pinArt(pin:WorldPin):Int=when(pin.kind) {
    "city"->R.drawable.castle_sprite
    "hunt"->R.drawable.world_beast
    else->when(pin.data.optString("node_type")){"food"->R.drawable.world_farm;"wood"->R.drawable.world_lumber;else->R.drawable.world_mine}
}

@Composable
fun KingdomMinimap(camera:MapCamera,pins:List<WorldPin>,onMove:(Offset)->Unit) {
    val terrain=ImageBitmap.imageResource(R.drawable.world_terrain)
    Canvas(Modifier.size(88.dp).clip(RoundedCornerShape(12.dp)).border(2.dp,Bronze,RoundedCornerShape(12.dp)).testTag("kingdom-minimap").pointerInput(Unit){detectTapGestures{onMove(Offset((it.x/size.width*499).coerceIn(0f,499f),(it.y/size.height*499).coerceIn(0f,499f)))}}) {
        drawImage(terrain,dstSize=IntSize(size.width.toInt(),size.height.toInt()))
        drawRect(Ink.copy(.3f))
        pins.forEach{drawCircle(if(it.kind=="city") Gold else if(it.kind=="hunt") Color(0xFFFF7E60) else Mint,2.dp.toPx(),Offset(it.x/499*size.width,it.y/499*size.height))}
        val c=Offset(camera.x/499*size.width,camera.y/499*size.height)
        val side=size.width*80/499/camera.zoom
        drawRect(Parchment,c-Offset(side/2,side/2),Size(side,side),style=Stroke(1.5.dp.toPx()))
        drawCircle(Gold,2.dp.toPx(),c)
    }
}
