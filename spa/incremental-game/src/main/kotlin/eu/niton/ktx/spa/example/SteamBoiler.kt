package eu.niton.ktx.spa.example

import eu.niton.ktx.spa.If
import eu.niton.ktx.spa.component
import eu.niton.ktx.spa.cx
import eu.niton.ktx.spa.example.utils.Process
import eu.niton.ktx.spa.example.utils.discovery
import eu.niton.ktx.tags.DivHtmlTag
import eu.niton.ktx.tags.div
import eu.niton.ktx.tags.span
import kotlin.math.max
import kotlin.time.Duration.Companion.seconds


fun DivHtmlTag<*>.SteamBoiler() = component {
    cx.createEffect {
        if (!game.discoveredSteamBoiler) return@createEffect
        val heatingStartTemp = 30
        val firePerHeat = 25.0
        val heatPerSteam = 100
        val heatBoiler = Process({ 1.seconds / (max(game.fire.value - heatingStartTemp, 0.1f) / firePerHeat) }) {
            game.heat.value += it
        }
        val dissipation = Process({ 5.seconds }) {
            game.heat.value *= (1 - (0.1f * it))
        }
        val boilWater = Process({ 1.seconds / (game.heat.value / heatPerSteam).toDouble() }) {
            game.water.value -= it
            game.steam.value += it
        }
        cx.createEffect {
            if (game.heat.value >= 30 && game.water.value >= 1) boilWater.start()
            else boilWater.stop()
        }
        heatBoiler.start()
        dissipation.start()
        cx.cleanup {
            heatBoiler.stop()
            dissipation.stop()
            boilWater.stop()
        }
    }
    ResourceBox(title = { "Boiler" }) {
        div(`class` = { "flex flex-col gap-1" }) {
            div(`class` = { "$flexRow gap-2" }) {
                span(`class` = { "text" }) {
                    +"Heat"
                }
                ProgressBar(value = { game.heat.value }, max = { game.heat.max }, color = { "bg-orange-300" })
            }
            div(`class` = { "$flexRow gap-2" }) {
                span(`class` = { "text" }) {
                    +"Water"
                }
                ProgressBar(value = { game.water.value }, max = { game.water.max }, color = { "bg-blue-600" })
            }
            div(`class` = { "$flexRow gap-2" }) {
                span(`class` = { "text" }) {
                    +"Steam"
                }
                ProgressBar(value = { game.steam.value }, max = { game.steam.max }, color = { "bg-blue-100" })
            }
        }
    }
}
