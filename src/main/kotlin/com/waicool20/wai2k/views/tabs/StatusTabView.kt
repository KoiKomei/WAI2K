/*
 * GPLv3 License
 *
 *  Copyright (c) WAI2K by waicool20
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.waicool20.wai2k.views.tabs

import com.waicool20.wai2k.Wai2k
import com.waicool20.wai2k.events.EventBus
import com.waicool20.wai2k.events.ScriptStatsUpdateEvent
import com.waicool20.wai2k.script.ScriptRunner
import com.waicool20.wai2k.script.ScriptStats
import com.waicool20.wai2k.util.formatted
import com.waicool20.waicoolutils.divAssign
import com.waicool20.waicoolutils.javafx.CoroutineScopeView
import com.waicool20.waicoolutils.plusAssign
import javafx.scene.control.Label
import javafx.scene.layout.VBox
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

class StatusTabView : CoroutineScopeView() {
    override val root: VBox by fxml("/views/tabs/status-tab.fxml")

    private val startTimeLabel: Label by fxid()
    private val elapsedTimeLabel: Label by fxid()

    private val logisticsSentLabel: Label by fxid()
    private val logisticsReceivedLabel: Label by fxid()
    private val sortiesDoneLabel: Label by fxid()
    private val sphLabel: Label by fxid()
    private val repairsLabel: Label by fxid()
    private val sprLabel: Label by fxid()

    private val enhancementsDoneLabel: Label by fxid()
    private val dollsUsedForEnhancementLabel: Label by fxid()
    private val disassemblesDoneLabel: Label by fxid()
    private val dollsUsedForDisassemblyLabel: Label by fxid()

    private val equipDisassemblesDoneLabel: Label by fxid()
    private val equipsUsedForDisassemblyLabel: Label by fxid()

    private val combatReportsWrittenLabel: Label by fxid()
    private val simulationEnergyUsedLabel: Label by fxid()
    private val coalitionEnergyUsedLabel: Label by fxid()
    private val gameRestartsLabel: Label by fxid()

    private val timersLabel: Label by fxid()

    private val scriptRunner get() = Wai2k.scriptRunner

    init {
        title = "Status"
    }

    override fun onDock() {
        super.onDock()
        EventBus.subscribe<ScriptStatsUpdateEvent>()
            .onEach { updateScriptStats(it.stats) }
            .launchIn(this)

        launch(CoroutineName("Status Tab View Updater")) {
            while (isActive) {
                updateView()
                delay(1000)
            }
        }
    }

    private fun updateView() {
        updateTimes()
        updateEchelonStats()
    }

    private fun updateTimes() {
        startTimeLabel.text = scriptRunner.lastStartTime?.formatted() ?: ""
        elapsedTimeLabel.text =
            formatDuration(Duration.of(scriptRunner.elapsedTime, ChronoUnit.MILLIS))
    }

    private fun updateScriptStats(stats: ScriptStats) = with(stats) {
        logisticsSentLabel.text = "$logisticsSupportSent"
        logisticsReceivedLabel.text = "$logisticsSupportReceived"
        sortiesDoneLabel.text = "$sortiesDone"
        sphLabel.text = formatDecimal(sortiesDone / (scriptRunner.elapsedTime.toDouble() / 1000.0 / 3600.0))
        repairsLabel.text = "$repairs"
        sprLabel.text = formatDecimal(sortiesDone / repairs.toDouble())

        enhancementsDoneLabel.text = "$enhancementsDone"
        dollsUsedForEnhancementLabel.text = "$dollsUsedForEnhancement"
        disassemblesDoneLabel.text = "$disassemblesDone"
        dollsUsedForDisassemblyLabel.text = "$dollsUsedForDisassembly"

        equipDisassemblesDoneLabel.text = "$equipDisassemblesDone"
        equipsUsedForDisassemblyLabel.text = "$equipsUsedForDisassembly"

        combatReportsWrittenLabel.text = "$combatReportsWritten"
        simulationEnergyUsedLabel.text = "$simEnergySpent"
        coalitionEnergyUsedLabel.text = "$coalitionEnergySpent"
        gameRestartsLabel.text = "$gameRestarts"
    }

    private fun updateEchelonStats() {
        val builder = StringBuilder()
        scriptRunner.gameState.apply {
            val echelonLogistics =
                echelons.filter { it.logisticsSupportAssignment?.eta?.isAfter(Instant.now()) == true }
                    .sortedBy { it.logisticsSupportAssignment?.eta }
            if (echelonLogistics.isNotEmpty()) {
                builder /= "Logistics:"
                builder += echelonLogistics.joinToString("\n") {
                    "\t- Echelon ${it.number} [${it.logisticsSupportAssignment?.logisticSupport?.formattedString}]: ${
                        timeDelta(
                            it.logisticsSupportAssignment?.eta
                        )
                    }"
                }
                builder.appendLine()
            }

            val echelonRepairs = echelons
                .flatMap { echelon -> echelon.members.map { echelon.number to it } }
                .filter { it.second.repairEta?.isAfter(Instant.now()) == true }
                .sortedBy { it.second.repairEta }
            if (echelonRepairs.isNotEmpty()) {
                builder /= "Repairs:"
                builder += echelonRepairs.joinToString(
                    "\n",
                    postfix = "\n"
                ) { (echelonNumber, member) ->
                    "\t- Echelon $echelonNumber [${member.slot}]: ${timeDelta(member.repairEta)}"
                }
            }

            if (Wai2k.profile.combatSimulation.enabled) {
                val combatSims = listOf(simEnergy, timeDelta(simNextCheck))
                    .filter { simNextCheck >= Instant.now() }
                if (combatSims.isNotEmpty()) {
                    builder /= "Combat Energy ETA:"
                    builder += "\t- ${combatSims[0]}/12 : ${combatSims[1]}\n"
                }
            }

            if (Wai2k.profile.combatSimulation.coalition.enabled) {
                val coalSims = listOf(coalitionEnergy, timeDelta(simNextCheck))
                    .filter { simNextCheck >= Instant.now() }
                if (coalSims.isNotEmpty()) {
                    builder /= "Coalition Energy ETA:"
                    builder += "\t- ${coalSims[0]}/12 : ${coalSims[1]}\n"
                }
            }

            if (Wai2k.profile.combatReport.enabled
                && Wai2k.profile.combat.enabled
                && scriptRunner.state != ScriptRunner.State.STOPPED
            ) {
                if (reportsNextCheck > Instant.now()) {
                    builder /= "Combat Report ETA:"
                    builder += "\t - ${timeDelta(reportsNextCheck)}\n"
                }
                if (builder.isEmpty()) {
                    builder += "Nothing is going on right now!"
                }
            }
        }
        timersLabel.text = builder.toString()
    }

    private fun timeDelta(time: Instant?): String {
        val duration = time?.let { Duration.between(it, Instant.now()).abs() }
            ?: return "00:00:00"
        return formatDuration(duration) ?: "00:00:00"
    }

    private fun formatDuration(duration: Duration?) = duration?.seconds?.let {
        String.format("%02d:%02d:%02d", it / 3600, (it % 3600) / 60, it % 60)
    }

    private fun hoursSince(time: Instant?) =
        time?.let { Duration.between(it, Instant.now()).seconds / 3600.0 } ?: 0.0

    private fun formatDecimal(d: Double) = DecimalFormat("0.00").format(d).replace("\uFFFD", "0.00")
}
