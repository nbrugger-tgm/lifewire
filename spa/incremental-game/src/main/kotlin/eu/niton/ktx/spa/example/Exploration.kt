package eu.niton.ktx.spa.example

import eu.niton.ktx.spa.If
import eu.niton.ktx.spa.createSignal
import eu.niton.ktx.spa.cx
import eu.niton.ktx.spa.example.utils.Process
import eu.niton.ktx.spa.example.utils.localStorageStringSignal
import eu.niton.ktx.tags.DivHtmlTag
import eu.niton.ktx.tags.div
import eu.niton.ktx.tags.span
import kotlin.time.Duration.Companion.seconds

fun DivHtmlTag<*>.Exploration() {
    If({ game.exploring }) {
        Explorer()
    }
    val mushrooms = Resource("mushrooms", 8)
    var mushroomGrow by createSignal(0f);
    val mushroomProgress = Process({90.seconds}, {mushroomGrow+=it})
    cx.createEffect {
        if(mushroomGrow>=1f) {
            mushroomGrow -= 1f
            mushrooms.value += 1
        }
    }
    cx.createEffect {
        if(mushrooms.value < mushrooms.max) mushroomProgress.start()
        else mushroomProgress.stop()
    }
    ResourceBox(title = { "The Forest" }, `class` = {"flex flex-col gap-2"}) {
        Button(onClick = { game.exploring = true }) { +"Explore" }
        div(`class` = { "flex flex-row text gap-1 justify-between" }) {
            div(`class` = {"content-center"}) { span { +"Mushroom" } }
            div(`class` = {"items-end flex flex-col"}){
                ProgressBar(
                    value = { mushroomGrow },
                    max = { 1 },
                    displayText = { false },
                    color = { "bg-green-300" },
                    insideText = {
                        if(mushroomProgress.isRunning) "growing"
                        else null
                    })
                ProgressBar(
                    value = { mushrooms.value },
                    max = { mushrooms.max },
                    displayText = { true },
                    color = { "bg-orange-300" })
            }
        }
    }
}

fun DivHtmlTag<*>.Explorer() {
    var zone by localStorageStringSignal("exploration_zone", "Forest")
    var dialogText by localStorageStringSignal("exploration_dialog_text", "You enter the forest")
    val inventory = cx.createSignal(mapOf<Resource, Float>())
    val inventoryMax = 20
    div(`class` = { fullScreenOverlay }) {
        div(`class` = { "w-fit p-4 border bg-white rounded-xl flex flex-col gap-1" }) {
            div(`class` = { "flex flex-row justify-between" }) {
                span(`class` = { "text-3xl" }) { +{ zone } }
                Button(onClick = { game.exploring = false }, `class` = { "bg-red-200" }) {
                    +"Run back home"
                }
            }
            div(`class` = { "border rounded bg-gray-50 text-md p-2" }) {
                TypingAnimation { dialogText }
            }
            div(`class` = { "flex flex-row gap-2" }) {
                div(`class` = {"flex flex-col gap-1"}) {
                    span(`class` = { "text-xl" }) { +"What will you do?" }
                    Button(onClick = {}){+"Kill"}
                    Button(onClick = {}){+"Eat"}
                    Button(onClick = {}){+"Sleep"}
                    Button(onClick = {}){+"???"}
                    Button(onClick = {}){+"More choices i guess"}
                }
                div(`class` = {"border-l"}){}
                div {
                    div(`class` = { "flex flex-row text-xl" }) {
                        +"Backpack "
                        +" "
                        ProgressBar(value = { inventory.values.sum() }, max = { inventoryMax }, displayText = { false })
                    }
                    inventory.onPut { res, value ->
                        div(`class` = { "flex flex-row justify-between" }) {
                            span { +res.name }
                            span { +{ value.toString() } }
                        }
                    }
                }
            }
        }
    }
}