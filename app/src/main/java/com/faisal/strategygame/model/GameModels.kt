package com.faisal.strategygame.model

data class Resources(
    val food: Long = 125_400,
    val wood: Long = 98_750,
    val stone: Long = 42_300,
    val gold: Long = 8_640,
    val gems: Long = 750,
)

data class Building(
    val id: String,
    val name: String,
    val level: Int,
    val icon: String,
    val description: String,
)

data class Commander(
    val id: String,
    val name: String,
    val rarity: String,
    val level: Int,
    val power: Int,
)

data class Mission(
    val id: String,
    val title: String,
    val difficulty: String,
    val reward: String,
    val completed: Boolean = false,
)

data class MailItem(
    val id: String,
    val title: String,
    val body: String,
    val read: Boolean = false,
)

data class GameAction(
    val type: String,
    val targetId: String,
    val label: String,
    val endsAt: Long,
)

data class GameState(
    val governorName: String = "",
    val power: Long = 24_850,
    val cityLevel: Int = 8,
    val resources: Resources = Resources(),
    val troops: Int = 2_450,
    val alliance: String = "Falcons",
    val allianceHelp: Int = 3,
    val buildings: List<Building> = listOf(
        Building("hall", "City Hall", 8, "🏰", "The heart of your kingdom"),
        Building("barracks", "Barracks", 7, "⚔️", "Train powerful infantry"),
        Building("academy", "Academy", 6, "📚", "Research kingdom technology"),
        Building("hospital", "Hospital", 5, "🏥", "Heal wounded troops"),
        Building("farm", "Farm", 7, "🌾", "Produces food"),
        Building("lumber", "Lumber Mill", 7, "🪵", "Produces wood"),
    ),
    val commanders: List<Commander> = listOf(
        Commander("c1", "Aurelius", "Legendary", 25, 8_400),
        Commander("c2", "Valeria", "Epic", 20, 5_850),
        Commander("c3", "Tariq", "Epic", 18, 4_920),
    ),
    val missions: List<Mission> = listOf(
        Mission("m1", "Defeat the border raiders", "Easy", "5,000 Food"),
        Mission("m2", "Scout the ancient ruins", "Medium", "200 Gems"),
        Mission("m3", "Protect the trade caravan", "Hard", "Commander XP"),
    ),
    val mail: List<MailItem> = listOf(
        MailItem("mail1", "Welcome, Governor!", "Your kingdom awaits your command."),
        MailItem("mail2", "Alliance reward", "Your alliance reached a new milestone."),
    ),
)
