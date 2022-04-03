package fr.lewon.dofus.bot.scripts.tasks.impl.transport

import fr.lewon.dofus.bot.core.dat.managers.TransportSortingUtil
import fr.lewon.dofus.bot.core.logs.LogItem
import fr.lewon.dofus.bot.core.model.maps.DofusMap
import fr.lewon.dofus.bot.scripts.tasks.BooleanDofusBotTask
import fr.lewon.dofus.bot.util.game.MoveUtil
import fr.lewon.dofus.bot.util.io.KeyboardUtil
import fr.lewon.dofus.bot.util.io.MouseUtil
import fr.lewon.dofus.bot.util.io.WaitUtil
import fr.lewon.dofus.bot.util.network.GameInfo
import java.awt.event.KeyEvent
import java.text.Normalizer

class ZaapTowardTask(private val zaap: DofusMap) : BooleanDofusBotTask() {

    override fun doExecute(logItem: LogItem, gameInfo: GameInfo): Boolean {
        val zaapDestinations = OpenZaapInterfaceTask().run(logItem, gameInfo)
        val zaapDestination = zaapDestinations
            .firstOrNull { it.getCoordinates() == zaap.getCoordinates() }
            ?: error("Could not find zaap destination [${zaap.getCoordinates().x} ; ${zaap.getCoordinates().y}]. Did you explore it with this character ?")

        zaapToDestination(gameInfo, zaapDestination, zaapDestinations)
        return true
    }

    private fun zaapToDestination(gameInfo: GameInfo, zaapDestination: DofusMap, zaapDestinations: List<DofusMap>) {
        val sortingMode = TransportSortingUtil.getZaapSortingMode()
        val regionButtonLocation = ZaapInterfaceUtil.getHeaderRegionButtonLocation()
        if (sortingMode.sortCriteria != "areaName") {
            MouseUtil.leftClick(gameInfo, regionButtonLocation, 500)
        }
        if (sortingMode.descendingSort) {
            MouseUtil.leftClick(gameInfo, regionButtonLocation, 500)
        }
        WaitUtil.sleep(1000)
        val orderedZaaps = getOrderedZaapDestinations(zaapDestinations)
        val zaapDestinationIndex = orderedZaaps.indexOf(zaapDestination)
        val firstElementLocation = ZaapInterfaceUtil.getFirstElementLocation()
        val tenthElementLocation = ZaapInterfaceUtil.getTenthElementLocation()
        MouseUtil.leftClick(gameInfo, firstElementLocation, 200)
        var skippedCount = 0
        while (zaapDestinationIndex - skippedCount >= 10) {
            MouseUtil.leftClick(gameInfo, tenthElementLocation)
            KeyboardUtil.sendKey(gameInfo, KeyEvent.VK_DOWN, 200)
            skippedCount += minOf(10, zaapDestinations.size - 10 - skippedCount)
        }
        val zaapLocation = firstElementLocation.also {
            it.y += ZaapInterfaceUtil.DELTA_ELEMENT * (zaapDestinationIndex - skippedCount)
        }
        gameInfo.eventStore.clear()
        WaitUtil.sleep(300)
        MouseUtil.tripleLeftClick(gameInfo, zaapLocation)
        MoveUtil.waitForMapChange(gameInfo)
    }

    private fun getOrderedZaapDestinations(zaapDestinations: List<DofusMap>): List<DofusMap> {
        val favoriteZaaps = TransportSortingUtil.getFavoriteZaapMapIds().map { it.toInt() }
        val favoriteIndexFunc: (DofusMap) -> String = { if (favoriteZaaps.contains(it.id.toInt())) "A" else "B" }
        return zaapDestinations.sortedBy { removeAccents(favoriteIndexFunc(it) + it.subArea.area.name + it.subArea.name) }
    }

    private fun removeAccents(str: String): String {
        val temp = Normalizer.normalize(str.lowercase(), Normalizer.Form.NFD)
        val regex = "\\p{InCombiningDiacriticalMarks}+".toRegex()
        return regex.replace(temp, "")
    }

    override fun onStarted(): String {
        val coordinates = zaap.getCoordinates()
        return "Zapping toward [${coordinates.x}, ${coordinates.y}] ..."
    }
}