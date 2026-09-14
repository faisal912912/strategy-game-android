package com.faisal.strategygame

/** Core training contract: tiers are training options; the army response is aggregated by type. */
data class TrainingCost(val food: Long, val wood: Long, val gold: Long, val seconds: Long)
fun tierBuildingLevel(tier: Int): Int { require(tier in 1..5); return if (tier == 1) 1 else tier * 5 }
fun trainingCost(tier: Int, amount: Long): TrainingCost {
    require(tier in 1..5 && amount in 1..100000)
    return TrainingCost(amount*10*tier, amount*5*tier, amount*2*tier, maxOf(10,amount*tier/10))
}
fun trainingBuilding(type: String) = when(type) { "infantry" -> "barracks"; "cavalry" -> "stable"; "archers" -> "archery_range"; else -> error("Unknown troop type") }

/** Coordinates remain in the server's world space regardless of zoom or screen density. */
data class MapCamera(val x: Float, val y: Float, val zoom: Float = 1f) {
    fun pan(dx: Float, dy: Float, pixelsPerUnit: Float, extent: Float) = copy(
        x=(x-dx/pixelsPerUnit).coerceIn(0f,extent), y=(y-dy/pixelsPerUnit).coerceIn(0f,extent))
    fun zoomAt(factor: Float, focusX: Float, focusY: Float, baseScale: Float, extent: Float): MapCamera {
        val z=(zoom*factor).coerceIn(.5f,4f)
        return MapCamera((x+focusX/baseScale*(1/zoom-1/z)).coerceIn(0f,extent),
            (y+focusY/baseScale*(1/zoom-1/z)).coerceIn(0f,extent),z)
    }
}
