package fr.lewon.dofus.bot.scripts

import fr.lewon.dofus.bot.scripts.impl.*

enum class DofusBotScripts(val buildScriptFun: () -> DofusBotScript) {

    TREASURE_HUNT_EXECUTE({ MultipleTreasureHuntScript() }),
    FIGHT_ARENA({ FightArenaScript() }),
    FIGHT_DUNGEON({ FightDungeonScript() }),
    SMITH_MAGIC({ SmithMagicScript() }),
    EXPLORE_CURRENT_AREA({ ExploreCurrentAreaScript() }),
    EXPLORE_AREA({ ExploreAreaScript() }),
    EXPLORE_ALL_ZAAPS({ ExploreAllZaapsScript() }),
    REACH_MAP({ ReachMapScript() }),
    TRAVEL({ TravelScript() }),
    INIT_ALL({ InitAllScript() }),
    PRINT_ALL_MAP_INFO({ PrintAllMapInfoScript() }),
    SHOW_D20_CONTENT({ ReadD2OFileScript() }),
    READ_SPELL_INFO({ ReadSpellInfoScript() }),
    READ_LABEL({ ReadLabelScript() }),
    TEST({ TestScript() });

    fun buildScript(): DofusBotScript {
        return buildScriptFun()
    }

}