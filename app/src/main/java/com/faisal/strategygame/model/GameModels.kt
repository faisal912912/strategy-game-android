package com.faisal.strategygame.model

data class Resources(
    val food: Long = 125_400,
    val wood: Long = 98_750,
    val stone: Long = 42_300,
    val gold: Long = 8_640,
)

data class Building(
    val name: String,
    val level: Int,
    val status: String,
)

data class CityState(
    val governorName: String = "Governor",
    val power: Long = 24_850,
    val cityLevel: Int = 8,
    val resources: Resources = Resources(),
    val buildings: List<Building> = listOf(
        Building("City Hall", 8, "Ready"),
        Building("Barracks", 7, "Training"),
        Building("Academy", 6, "Researching"),
        Building("Hospital", 5, "Ready"),
    ),
)
