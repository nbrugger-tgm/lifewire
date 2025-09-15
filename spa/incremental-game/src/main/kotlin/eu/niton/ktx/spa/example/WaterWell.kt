package eu.niton.ktx.spa.example

import eu.niton.ktx.spa.If
import eu.niton.ktx.spa.createSignal
import eu.niton.ktx.spa.cx
import eu.niton.ktx.spa.example.utils.Task
import eu.niton.ktx.spa.example.utils.discovery
import eu.niton.ktx.spa.example.utils.localStorageBooleanSignal
import eu.niton.ktx.spa.example.utils.localStorageIntSignal
import eu.niton.ktx.tags.DivContent
import eu.niton.ktx.tags.br
import eu.niton.ktx.tags.div
import eu.niton.ktx.tags.span
import kotlin.time.Duration.Companion.seconds



fun DivContent.WaterWell() {
    var waterPerClick by createSignal(3)
    var waterFetchTime by createSignal(4)
    cx.createEffect {
        if (game.bucket.isCrafted) {
            waterPerClick = 18
            waterFetchTime = 10
        } else if (game.bowl.isCrafted) {
            waterPerClick = 6
            waterFetchTime = 5
        }
    }
    If({ game.discoveredWaterWell }) {
        ResourceBox(title = { "Water Well" }, `class` = {"flex flex-col gap-1"}) {
            div(`class` = { "flex flex-col gap-1" }) {
                div(`class` = { "flex flex-row gap-1 justify-between" }) {
                    div { +"Water per click" }
                    div(`class` = { "bold" }) { +{ waterPerClick.toString() } }
                }
                div(`class` = { "flex flex-row gap-1 justify-between" }) {
                    div { +"Fetch Time" }
                    div(`class` = { "bold" }) { +{ waterFetchTime.toString() + "s" } }
                }
            }
            val fetchWater = Task({ waterFetchTime.seconds }) {
                game.water.value += waterPerClick
            }
            div(`class` = { "$flexRow gap-2" }) {
                Button(onClick = { fetchWater.perform() }) {
                    +"Fetch Water"
                }
                ProgressBar(
                    value = { fetchWater.percentDone * waterPerClick },
                    max = { waterPerClick },
                    displayText = { false },
                    color = { "bg-blue-400" })
            }
        }
    }
}
