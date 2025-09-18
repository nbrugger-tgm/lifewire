package eu.niton.ktx.incrementalgame

import eu.niton.ktx.StringContent
import eu.niton.ktx.spa.*
import eu.niton.ktx.incrementalgame.components.TextTooltip
import eu.niton.ktx.incrementalgame.components.Tooltip
import eu.niton.ktx.incrementalgame.utils.localStorageBooleanSignal
import eu.niton.ktx.incrementalgame.utils.localStorageFloatSignal
import eu.niton.ktx.incrementalgame.utils.localStorageIntSignal
import eu.niton.ktx.tags.*
import eu.niton.ktx.tags.content.render
import org.teavm.jso.browser.Window

val game by lazy { Game() }


fun main() {
    val mountPoint = document.getElementById("app")
    cx.run {
        println("Load game : $game")
        insert({ render(BodyContent::App) }, mountPoint)
    }
}


fun StringContent<*>.TypingAnimation(onDone:(()->Unit)?=null,speed:(()->Int) = {2},text: () -> String) {
    val displayedText = cx.createSignal("")

    cx.createEffect {
        var timeout: Int? = null;
        fun type(targetText: String) {
            if (targetText == displayedText.untracked) {
                onDone?.invoke()
                return;
            }
            if (targetText.startsWith(displayedText.untracked)) {
                displayedText.update { it + targetText.substringAfter(displayedText.untracked)[0] }
            } else {
                displayedText.set(displayedText.untracked.dropLast(1))
            }
            val delay = 100/speed()
            timeout = Window.setTimeout({ type(text()) }, delay + delay * Math.random())
        }
        type(text())
        cx.cleanup { if (timeout != null) Window.clearTimeout(timeout) }
    }
    +displayedText::get
}


val flexRow = "flex flex-row justify-between"

class Resource(val name: String, max: Int,defaultValue:Float?=null) {
    var value by localStorageFloatSignal(name, defaultValue ?: 0f)
    var max by localStorageIntSignal("${name}_max", max)

    init {
        cx.createEffect { if (value > max) value = this.max.toFloat() }
    }
}
data class Food(val resource: Resource, val nutrition:Int)

class Upgrade(name: String, val costs: Map<Resource, Float>) {
    private var crafted by localStorageBooleanSignal("${name}_upgrade", false)
    val isCrafted get() = crafted
    fun purchase() {
        println("try to purchase upgrade for $costs")
        if (crafted) {
            println("But it is already crafted")
            return
        }
        if (!canAfford()) {
            println("But yu cannot afford it")
            return
        }
        crafted = true
        costs.forEach { res, cost ->
            res.value -= cost
        }
    }

    private fun canAfford() = costs.isEmpty() || costs.all { (res, value) -> res.value >= value }
}

fun reset() {
    Window.current().localStorage.clear()
    Window.current().location.reload()
}

fun BodyContent.App() {
    script(src = { "https://cdn.jsdelivr.net/npm/@tailwindcss/browser@4" }) {}
    div(`class` = { "p-3" }) {
        h1(`class` = { "text-4xl inline" }) {
            +"Incremental adventure"
        }
        TextTooltip(text = { "Deletes all data!" }) {
            Button(`class` = { "rounded text" }, onClick = { reset() }) { +"Reset" }
        }
        div(`class` = { "p-1 border mb-2" }) {
            h3(`class` = { "text-xl p-0" }) {
                +"Dialog"
            }
            TypingAnimation { game.dialogText }
        }
        div(`class` = { "flex flex-row flex-wrap gap-2" }) {
            Toolbox()
            If({game.discoverForest}) {
                Exploration()
            }
            Bonfire()
            If({ game.discoveredSteamBoiler }) {
                SteamBoiler()
            }
            If({ game.discoveredWaterWell }) {
                WaterWell()
            }
            If({ game.discoveredMine }) {
                Mine()
            }
        }
    }
}

const val fullScreenOverlay =
    "absolute h-full w-full bg-gray-200/75 top-0 left-0 justify-center items-center content-center flex"

fun SpanHtmlTag<*>.PriceDisplay(costs: Map<Resource, Float>) {
    span(`class` = { "flex flex-col gap-1" }) {
        span(`class` = { "text-xl" }) { +"Price" }
        span(`class` = { "pl-2 flex flex-row gap-4" }) {
            span(`class` = { "flex flex-col text" }) {
                costs.forEach {
                    span { +it.key.name }
                }
            }
            span(`class` = { "flex flex-col text-green-400" }) {
                costs.forEach {
                    span { +{ it.value.toString() } }
                }
            }
        }
    }
}

fun DivHtmlTag<*>.Toolbox() = component {
    if (!game.discoveredWaterWell) return@component
    ResourceBox(title = { "Toolbox" }) {
        If({ game.discoveredOreWashing }) {
            div(`class` = { "flex flex-col gap-1" }) {
                Tooltip({
                    span(`class` = { "flex flex-col border-b border-black" }) {
                        span(`class` = { "text-xl" }) { +"Effect" }
                        span(`class` = { "text-green-600" }) {
                            +"Water per click: 6"
                        }
                        span(`class` = { "text-green-600" }) {
                            +"Fetch Time: 5s"
                        }
                    }
                    PriceDisplay(game.bucket.costs)
                }) {
                    Button(onClick = game.bowl::purchase, disabled = { game.bowl.isCrafted || game.bucket.isCrafted }) {
                        span(`class` = { "text-pretty" }) {
                            +{
                                if (game.bucket.isCrafted) "You already crafted a better bucket"
                                else if (game.bowl.isCrafted) "Bowl crafted"
                                else "Form nails from a metal chunk and craft a bowl from the wood around you"
                            }
                        }
                    }
                }
                Button(onClick = game.bucket::purchase, disabled = game.bucket::isCrafted) {
                    span(`class` = { "text-pretty" }) {
                        +{
                            if (!game.bucket.isCrafted) "Form nails from 3 metal chunks and craft a bucket from the wood around you"
                            else "Bucket crafted"
                        }
                    }
                    br()
                    span(`class` = { "text-green-600" }) {
                        +"Water per click: 18"
                        br()
                        +"Fetch Time: 10s"
                    }
                }
            }
        }
    }
}

fun DivHtmlTag<*>.ProgressBar(
    value: () -> Float,
    max: () -> Int,
    color: () -> String = { "bg-green-400" },
    displayText: () -> Boolean = { true },
    insideText: (()->String?)? = null,
    `class`: (() -> String)?=null
) = component {
    val valueStr = { value().toInt().toString() }
    val maxStr = { max().toString() }
    val bar: DivHtmlTag<*>.(`class`: (() -> String)?)->Unit = { `class` ->
        div(`class` = { "border-1 min-w-[100px] ${`class`?.invoke()?:""}" }) {
            If({ insideText != null && insideText() != null }) {
                span(`class` = { "absolute" }) { +{ insideText?.invoke() ?: "" } }
            }
            div(
                `class` = { " min-h-3 height-full h-full ${color()} text-white" },
                style = { "width: ${100 * (value() / max().toDouble())}%;" }) {
                If({ insideText != null && insideText() != null }) {
                    +{ insideText?.invoke() ?: "" }
                }
            }
        }
    }
    if(displayText()) {
        div(`class` = { "flex flex-row gap-2 ${`class`?.invoke()?:""}" }) {
            span {
                +{ valueStr().padStart(maxStr().length) }
                +"/"
                +maxStr
            }
            bar({ "grow" })
        }
    } else {
        bar(`class`)
    }
}

fun DivHtmlTag<*>.ResourceBox(title: () -> String, `class`: (() -> String)? = null, body: DivBody) = component {
    div(`class` = { "p-2 border ${`class`?.invoke() ?: ""}" }) {
        div(`class` = { "text-xl font-bold font-serif p-0" }) {
            +title
        }
        body()
    }
}

fun ButtonHtmlTag<*>.Button(
    `class`: (() -> String)? = null,
    disabled: (() -> Boolean)? = null,
    onClick: () -> Unit,
    body: ButtonBody
) {
    button(
        `class` = { "${if (disabled?.invoke() ?: false) null else "cursor-pointer"} rounded border bg-gray-200 text-sm p-1 ${`class`?.invoke()}" },
        onClick = { onClick() },
        disabled = disabled,
        body = body
    )
}