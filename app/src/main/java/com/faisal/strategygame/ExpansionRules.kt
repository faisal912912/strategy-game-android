package com.faisal.strategygame

import org.json.JSONObject

fun speedupCompatible(type:String,jobKind:String) = type=="speedup" || type=="speedup_$jobKind"
fun speedupLimit(remainingMillis:Long,seconds:Long,owned:Long):Long {
    if(remainingMillis<=0 || seconds !in 1..86400 || owned<=0) return 0
    val duration=seconds*1000
    return minOf(10000,owned,1+(remainingMillis-1)/duration)
}
fun inventoryCategory(item:JSONObject):String = when(val type=item.optString("type")) {
    "food","wood","stone","gold" -> "resources"
    else -> if(type.startsWith("speedup")) "speedups" else "other"
}
fun speedupCommand(job:CityJob,itemKey:String,count:Long) = Command(
    "تسريع ${job.label}","/expansion/v1/speedup",
    com.faisal.strategygame.data.json("kind" to job.kind,"job_id" to job.jobId,"item_key" to itemKey,"count" to count))
