package eu.niton.ktx.spa.example

import eu.niton.ktx.spa.component
import eu.niton.ktx.spa.example.utils.Process
import eu.niton.ktx.spa.example.utils.localStorageIntSignal
import eu.niton.ktx.tags.DivContent
import eu.niton.ktx.tags.div
import eu.niton.ktx.tags.span
import kotlin.math.max
import kotlin.time.Duration.Companion.minutes

fun DivContent.Bonfire() = component {
    val fire = game.fire;
    var fireDissipation by localStorageIntSignal("fire_dissipation", 500)
    val dissipateFire = Process({ 1.minutes / fireDissipation }) {
        fire.value = max(0f, fire.value - (1 * it))
    }
    dissipateFire.start()
    ResourceBox(title = { "Bonfire" }) {
        ProgressBar(value = { fire.value }, max = { fire.max }, color = { "bg-red-400" })
        Button(onClick = { fire.value += 3 }, `class` = {"w-full my-1"}) {
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