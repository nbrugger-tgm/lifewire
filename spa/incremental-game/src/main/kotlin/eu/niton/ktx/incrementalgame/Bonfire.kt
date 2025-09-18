package eu.niton.ktx.incrementalgame

import eu.niton.ktx.incrementalgame.utils.Process
import eu.niton.ktx.incrementalgame.utils.localStorageIntSignal
import eu.niton.ktx.spa.component
import eu.niton.ktx.spa.createMemo
import eu.niton.ktx.spa.cx
import eu.niton.ktx.tags.DivContent
import eu.niton.ktx.tags.div
import eu.niton.ktx.tags.span
import kotlin.math.max
import kotlin.time.Duration.Companion.minutes

fun DivContent.Bonfire() = component {
    val fire = game.fire;
    val wood = game.wood;
    var fireDissipation by localStorageIntSignal("fire_dissipation", 500)
    val dissipateFire = Process({ 1.minutes / fireDissipation }) {
        fire.value = max(0f, fire.value - (1 * it))
    }
    val fireIntensityBracked by createMemo { (fire.value / 10).toInt() }
    val useWood = Process({1.minutes/(1+fireIntensityBracked)}) {
        if(wood.value > 0) wood.value = max(0f, wood.value-it)
        if(wood.value == 0f) {
            fire.value = 0f
        }
    }
    cx.createEffect {
        if(fire.value > 0) {
            useWood.start()
            dissipateFire.start()
        }
        else {
            useWood.stop()
            dissipateFire.stop()
        }
    }
    ResourceBox(title = { "Bonfire" }) {
        ProgressBar(value = { fire.value }, max = { fire.max }, color = { "bg-red-400" }, displayText = {false})
        Button(onClick = { fire.value += 3 }, `class` = {"w-full my-1 ${if(wood.value==0f)"text-gray-500" else ""}"}, disabled = {wood.value==0f}) {
            +{ if (fire.value > 10) "Heat up the fire" else "Ignite the flame" }
        }
        div(`class` = { "flex flex-row text gap-1 justify-between" }) {
            span { +"Wood" }
            ProgressBar(
                value = { game.wood.value },
                max = { game.wood.max },
                displayText = { true },
                color = { "bg-orange-800" })
        }
    }
}