package eu.niton.ktx.incrementalgame

import eu.niton.ktx.incrementalgame.utils.Task
import eu.niton.ktx.incrementalgame.utils.localStorageBooleanSignal
import eu.niton.ktx.incrementalgame.utils.localStorageFloatSignal
import eu.niton.ktx.incrementalgame.utils.localStorageIntSignal
import eu.niton.ktx.spa.If
import eu.niton.ktx.spa.component
import eu.niton.ktx.tags.DivContent
import eu.niton.ktx.tags.div
import eu.niton.ktx.tags.span
import kotlin.math.log2
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds



fun DivContent.Mine() = component {
    var orewashingFailed by localStorageBooleanSignal("ore_wash_failed", false)
    var depth by localStorageIntSignal("mine_depth", 0)
    var miningYield by localStorageIntSignal("mining_yield", 1)
    var miningDuration by localStorageFloatSignal("mining_duration", 10f)
    var steamConsumption by localStorageFloatSignal("mine_steam_consumption", 1f)

    val washingSuccess = { 0.10f + log2(depth.toFloat()) / 20 }

        val manualMine = Task({ (miningDuration * 1000).toInt().milliseconds }) {
            game.dirt.value += 1
            depth += 1
        }

        fun mine() {
            val steamCost = steamConsumption * miningDuration
            if (game.steam.value <= steamCost) return;
            game.steam.value -= steamCost
            manualMine.perform()
        }
        //TODO: Technically a bug since if discoveredMine get false and true it would mine multiple times
        ResourceBox(title = { "Mines" }) {
            div(`class` = { "flex flex-col gap-1" }) {


                If({ game.discoveredOreWashing && orewashingFailed }) {
                    div {
                        +{ "Depth: $depth" }
                    }
                    div {
                        +{ "Metal chance: ${(washingSuccess() * 100).toInt()}%" }
                    }
                }
                div(`class` = { flexRow }) {
                    Button(onClick = ::mine) { +"Pull lever" }
                    ProgressBar(value = { manualMine.percentDone }, max = { 1 }, displayText = { false })
                }

                div(`class` = { flexRow }) {
                    span { +"Dirt" }
                    ProgressBar(value = { game.dirt.value }, max = { game.dirt.max }, color = { "bg-orange-800" })
                }
                If({ game.discoveredOreWashing }) {

                    div(`class` = { flexRow }) {
                        span { +"Metal" }
                        ProgressBar(value = { game.metal.value }, max = { game.metal.max }, color = { "bg-gray-600" })
                    }
                    val oreWash = Task({ 10.seconds }) {
                        if (Math.random() < washingSuccess()) {
                            game.metal.value += 1
                        } else if (!orewashingFailed) {
                            orewashingFailed = true
                            game.dialogText =
                                "Sadly you realize that not every pile of dug up dirt contains metal. Maybe deeper down the density is better ..."
                        }
                    }
                    div(`class` = {"$flexRow gap-2"}) {
                        Button(onClick = {
                            if (game.water.value >= 5 && game.dirt.value >= 1) {
                                game.water.value -= 5
                                game.dirt.value -= 1
                                oreWash.perform()
                            }
                        }) {
                            +"Wash some dirt"
                        }
                        ProgressBar(value = oreWash::percentDone, max = { 1 }, displayText = { false })
                    }
                }
            }

    }
}