package fr.lewon.dofus.bot.scripts.impl

import fr.lewon.dofus.bot.core.logs.LogItem
import fr.lewon.dofus.bot.scripts.*
import fr.lewon.dofus.bot.scripts.tasks.impl.hunt.ExecuteHuntTask
import fr.lewon.dofus.bot.scripts.tasks.impl.hunt.FetchHuntTask
import fr.lewon.dofus.bot.scripts.tasks.impl.transport.ReachMapTask
import fr.lewon.dofus.bot.scripts.tasks.impl.windows.RestartGameTask
import fr.lewon.dofus.bot.util.FormatUtil
import fr.lewon.dofus.bot.util.game.TreasureHuntUtil
import fr.lewon.dofus.bot.util.io.WaitUtil
import fr.lewon.dofus.bot.util.network.GameInfo

object MultipleTreasureHuntScript : DofusBotScript("Multiple treasure hunts") {

    private val resumeHuntParameter = DofusBotScriptParameter(
        "resume_hunt", "Set to true if you wish to resume an ongoing hunt", "false", DofusBotScriptParameterType.BOOLEAN
    )

    private val huntCountParameter = DofusBotScriptParameter(
        "hunt_count", "Amount of hunts to process before stopping", "50", DofusBotScriptParameterType.INTEGER
    )

    private val cleanCacheParameter = DofusBotScriptParameter(
        "clean_cache_every", "Amount of hunt(s) before cleaning Dofus cache", "12", DofusBotScriptParameterType.INTEGER
    )

    override fun getParameters(): List<DofusBotScriptParameter> {
        return listOf(
            resumeHuntParameter,
            huntCountParameter,
            cleanCacheParameter
        )
    }

    private val huntFetchDurations = ArrayList<Long>()
    private val huntDurations = ArrayList<Long>()
    private val successRateStat = DofusBotScriptStat("Success rate")
    private val averageHuntFetchDurationStat = DofusBotScriptStat("Average hunt fetch duration")
    private val averageHuntDurationStat = DofusBotScriptStat("Average hunt duration")
    private val nextRestartInStat = DofusBotScriptStat("Next restart in")

    override fun getStats(): List<DofusBotScriptStat> {
        return listOf(
            successRateStat,
            averageHuntFetchDurationStat,
            averageHuntDurationStat,
            nextRestartInStat
        )
    }

    override fun getDescription(): String {
        val resumeHunt = resumeHuntParameter.value.toBoolean()
        val huntCount = huntCountParameter.value.toInt()
        val cleanCacheEvery = cleanCacheParameter.value.toInt()
        var description = "Executes $huntCount hunt(s) starting with the current treasure hunt by : \n"
        if (!resumeHunt) description += " - Reaching treasure hunt start location \n"
        description += " - Finding hints and resolving treasure hunt steps \n"
        description += " - Fighting the chest at the end \n"
        description += "Dofus cache will be cleaned every $cleanCacheEvery hunt(s)"
        return description
    }

    override fun execute(logItem: LogItem, gameInfo: GameInfo, cancellationToken: CancellationToken) {
        clearStats()
        var successCount = 0
        var cleanCacheCount = cleanCacheParameter.value.toInt()

        if (TreasureHuntUtil.isFightStep(gameInfo)) {
            TreasureHuntUtil.fight(logItem, gameInfo, cancellationToken)
        }

        for (i in 0 until huntCountParameter.value.toInt()) {
            nextRestartInStat.value = "$cleanCacheCount hunt(s)"
            val fetchStartTimeStamp = System.currentTimeMillis()
            if (!TreasureHuntUtil.isHuntPresent(gameInfo)) {
                FetchHuntTask().run(logItem, gameInfo, cancellationToken)
            }
            val fetchDuration = System.currentTimeMillis() - fetchStartTimeStamp
            huntFetchDurations.add(fetchDuration)
            averageHuntFetchDurationStat.value = FormatUtil.durationToStr(huntFetchDurations.average().toLong())

            val huntStartTimeStamp = System.currentTimeMillis()

            if (!ReachMapTask(TreasureHuntUtil.getLastHintMap(gameInfo)).run(logItem, gameInfo, cancellationToken)) {
                error("Couldn't reach hunt start")
            }
            val success = ExecuteHuntTask().run(logItem, gameInfo, cancellationToken)
            WaitUtil.sleep(300)

            val huntDuration = System.currentTimeMillis() - huntStartTimeStamp
            huntDurations.add(huntDuration)
            averageHuntDurationStat.value = FormatUtil.durationToStr(huntDurations.average().toLong())

            if (success) successCount++
            cleanCacheCount--
            successRateStat.value = "$successCount / ${i + 1}"
            nextRestartInStat.value = "$cleanCacheCount hunt(s)"
            if (cleanCacheCount == 0) {
                RestartGameTask().run(logItem, gameInfo, cancellationToken)
                cleanCacheCount = cleanCacheParameter.value.toInt()
            }
            if (!success) {
                WaitUtil.sleep(600 * 1000 - huntDuration)
                TreasureHuntUtil.giveUpHunt(gameInfo, cancellationToken)
            }
        }
    }

    private fun clearStats() {
        huntDurations.clear()
        successRateStat.resetValue()
        averageHuntDurationStat.resetValue()
        nextRestartInStat.resetValue()
    }
}