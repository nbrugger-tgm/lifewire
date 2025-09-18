package eu.niton.ktx.incrementalgame

import eu.niton.ktx.incrementalgame.Game.Exploration.Event
import eu.niton.ktx.incrementalgame.Game.Exploration.Zone
import eu.niton.ktx.incrementalgame.components.TextTooltip
import eu.niton.ktx.incrementalgame.utils.Process
import eu.niton.ktx.incrementalgame.utils.localStorageBooleanSignal
import eu.niton.ktx.incrementalgame.utils.localStorageIntSignal
import eu.niton.ktx.spa.*
import eu.niton.ktx.spa.invoke
import eu.niton.ktx.tags.DivContent
import eu.niton.ktx.tags.DivHtmlTag
import eu.niton.ktx.tags.div
import eu.niton.ktx.tags.span
import eu.nitonfx.signaling.api.MapSignal
import org.teavm.jso.browser.Window
import kotlin.math.min
import kotlin.time.Duration.Companion.seconds

fun DivHtmlTag<*>.Exploration() {
    val mushrooms = game.mushrooms
    var mushroomGrow by createSignal(0f);
    val mushroomProgress = Process({ 90.seconds }, { mushroomGrow += it })
    cx.createEffect {
        if (mushroomGrow >= 1f) {
            mushroomGrow -= 1f
            mushrooms.value += 1
        }
    }
    cx.createEffect {
        if (mushrooms.value < mushrooms.max) mushroomProgress.start()
        else mushroomProgress.stop()
    }

    If({ game.exploring }) {
        ExplorerOverlay()
    }
    ResourceBox(title = { "The Forest" }, `class` = { "flex flex-col gap-2" }) {
        Button(onClick = { game.exploring = true; }) { +"Explore" }
        div(`class` = { "flex flex-row text gap-1 justify-between" }) {
            div(`class` = { "content-center" }) { span { +"Mushroom" } }
            div(`class` = { "items-end flex flex-col" }) {
                ProgressBar(
                    value = { mushroomGrow },
                    max = { 1 },
                    displayText = { false },
                    color = { "bg-green-300" },
                    insideText = {
                        if (mushroomProgress.isRunning) "growing"
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

private fun DivHtmlTag<*>.ExplorerOverlay() {
    val inventory = cx.createSignal(mapOf<Resource, Float>())
    val inventoryMax = 20
    var preparing by game.exploration::preparing

    inventory.onPut { k, v ->
        if (v() < 0) inventory[k] = 0f
    }
    inventory.onPut { k, v ->
        if (v() > 0) Window.current().localStorage.setItem("backpack_${k.name}", v().toString())
        else Window.current().localStorage.removeItem("backpack_${k.name}")
    }
    for (i in 0..<Window.current().localStorage.length) {
        val key = Window.current().localStorage.key(i)
        if (key.startsWith("backpack")) {
            val resName = key.substringAfter('_')
            inventory[game.resources[resName]] = Window.current().localStorage.getItem(key).toFloat()
        }
    }

    fun startAdventure() {
        inventory.forEach { resource, amount ->
            resource.value -= amount
        }
        game.exploration.preparing = false
        game.exploration.stamina = 0f
        game.exploration.dialogText = "You enter the forest"
        game.exploration.zone = Zone.FOREST
        game.exploration.event = Event.NONE
    }
    div(`class` = { fullScreenOverlay }) {
        div(`class` = { "w-fit p-4 border bg-white rounded-xl flex flex-col gap-1 xl:max-w-96" }) {
            If({ preparing }) {
                ExplorationPreparation(
                    inventory = inventory,
                    inventoryMax = { inventoryMax },
                    onDone = ::startAdventure
                )
            }.Else {
                Exploration(inventory, { inventoryMax }, onEnd = {
                    preparing = true
                    println("PREPARE AGAIN!? $preparing")
                    game.exploring = false
                })
            }
        }
    }
}

private fun DivHtmlTag<*>.ExplorationPreparation(
    inventory: MapSignal<Resource, Float>,
    inventoryMax: () -> Int,
    onDone: () -> Unit
) = component {
    val inventoryHasSpace by createMemo { inventory.values.sum() < inventoryMax() }
    div(`class` = { "flex flex-col gap-2" }) {
        div(`class` = { "flex flex-row justify-between gap-4" }) {
            span(`class` = { "text-2xl" }) {
                +"Prepare!"
            }
            Button(onClick = { game.exploring = false }) {
                +"Go back to camp"
            }
        }
        span {
            +"How much food will you pack up? You need food to explore further"
        }
        div(`class` = { "flex flex-row gap-2" }) {
            TextTooltip(text = { "Your backpack has limited size, the more food you take with you the less capacity to carry home you have" }) {
                +"Backpack"
            }
            ProgressBar(
                `class` = { "grow" },
                value = { inventory.values.sum() },
                max = inventoryMax,
                displayText = { false })
        }
        div(`class` = { "border-b border-gray-300" }) {}
        div(`class` = { "flex flex-col gap-1" }) {
            For({ game.foods }) { name, food ->
                div(`class` = { "flex flex-row gap-2 items-center" }) {
                    +name
                    div(`class` = { "flex flex-row gap-1" }) {
                        span(
                            `class` = { "border text-sm px-1 bg-gray-100 cursor-pointer" },
                            onClick = { inventory[food.resource] = inventory[food.resource]?.minus(1f) ?: 0f }) { +"-" }
                        ProgressBar(
                            value = { inventory[food.resource] ?: 0f },
                            max = { food.resource.value.toInt() },
                            displayText = { false },
                            insideText = { (inventory[food.resource] ?: 0).toString() },
                            color = { "bg-orange-700" }
                        )
                        span(
                            `class` = { "border text-sm px-1 bg-gray-100 cursor-pointer" },
                            onClick = {
                                if (inventoryHasSpace && ((inventory[food.resource] ?: 0f) < food.resource.value))
                                    inventory[food.resource] = inventory[food.resource]?.plus(1f) ?: 1f
                            }) { +"+" }
                    }
                }
            }
        }
        div(`class` = { "border-b border-gray-300" }) {}
        div {
            Button(onClick = onDone) { +"Start Adventure" }
        }
    }
}

private fun DivContent.Exploration(
    inventory: MapSignal<Resource, Float>,
    inventoryMax: () -> Int,
    onEnd: ()->Unit
) = component {
    var stamina by game.exploration::stamina
    var zone by game.exploration::zone
    var dialogText by game.exploration::dialogText
    var event by game.exploration::event
    var dist by createSignal(0)
    var ravensFed by localStorageIntSignal("fed_ravens", 0)
    var groundSearched by localStorageBooleanSignal("ground_searched",false)
    var crowFed by localStorageBooleanSignal("crow_fed",false)
    val eventChances by createMemo {
        val eventChance = (1 - (1 / (dist / 4 + 1)))
        val map = mutableMapOf<Event, Float>()
        val noEventChance = 1f - eventChance
        map[Event.NONE] = noEventChance
        map[Event.GOOSE_WAS_NEVER_AN_OPTION] = 0.2f * eventChance
        map[Event.WOODPILE] = 0.4f * eventChance
        map[Event.RAVEN] = 0.4f/(ravensFed+1) * eventChance
        map
    }

    inventory.onPut { k,v ->
        if (v() > k.max-k.value) inventory[k] = k.max-k.value
    }

    fun rollEvent(): Event {
        var pointer = 0f
        val roll = Math.random()
        eventChances.forEach { (event, chance) ->
            pointer += chance
            if(roll<pointer) return event
        }
        throw IllegalStateException("No event rolled?!")
    }

    val freeSpace = {inventoryMax()-inventory.values.sum()}
    fun addToInventory(res:Resource,berries: Float) {
        min(freeSpace(), berries).let {
            inventory[res] = inventory[res]?.plus(it) ?: it
        }
    }
    fun searchGround(){
        groundSearched = true
        val roll = Math.random()

        if(roll > 1-0.4) {
            dialogText = "Beneath the leaves there were a few loose branches"
            addToInventory(game.wood, (2f+(3*Math.random()).toInt()))
        } else if(roll > 1-0.4-0.2) {
            dialogText = "You notice that the ground is soft, might be similar to clay."
            addToInventory(game.clay, 1f)
        } else if(roll > 1f-0.4f-0.2f-0.15f) {
            val berries = (1f + (3f * Math.random()).toInt())
            dialogText = "You find and harvest a berry bush with $berries berries"
            addToInventory(game.berries,berries)
        } else {
            dialogText = "There seems to be nothing of use to you ..."
        }
    }

    fun feedCrow(res: Map.Entry<String,Food>) {
        crowFed = true
        dialogText = "As soon as you throw the crow a pice of ${res.key}, the crow flies down picks it off the ground and disapears into the forest"
        ravensFed += 1
    }

    fun doStep() {
        stamina -= 1
        //TODO: Zone
        dist++
        //FOREST ZONE
        event = rollEvent()
        when(event) {
            Event.GOOSE_WAS_NEVER_AN_OPTION -> {
                dialogText = "You hear a goose screaming. \"In the forest?\" you think as you suddenly see it running towards you. .... with a knive in its mouth!? And its shouting at you \"PEACE WAS NEVER AN OPTION\""
            }
            Event.WOODPILE -> {
                dialogText = "You find a tree that fell and burst into many pieces, that will be a lot to carry!"
            }
            Event.RAVEN -> {
                dialogText = "As you search the forest you spot a crow looking hungry at your food"
                crowFed = false
            }
            Event.NONE -> {
                dialogText = "You wander deeper into the forest"
                groundSearched = false
            }
        }
    }

    fun endRun() {
        try {
            inventory.forEach { (k, v) ->
                k.value += v
            }
            inventory.clear()
            onEnd()
        }catch(e: Exception) {
            println("WHY DO WE FAIL???")
            e.printStackTrace()
            println(e.message)
            e.printStackTrace(System.out)
        }
    }


    fun collectWoodPile(){
        while(stamina > 0.5f && inventory.values.sum() < inventoryMax()) {
            stamina -= 0.5f
            inventory[game.wood] = inventory[game.wood]?.plus(1)?:1f
        }
    }

    fun eat(res: Resource) {
        game.foods.values.find { it.resource == res }?.let {
            game.exploration.stamina += it.nutrition
            inventory[it.resource] = inventory[it.resource]?.minus(1)
        }
    }
    val hasFood by createMemo { game.foods.values.map { inventory[it.resource] ?: 0f }.sum() > 0f }
    cx.createEffect {
        if (stamina == 0f && !hasFood) dialogText = "As your legs grow weak you realize that your food supplies are used up..."
    }
    val playerChoices by createMemo {
        val choices = mutableMapOf<String, ()->Unit>()
        if (stamina == 0f && !hasFood) {
            for (i in 1..5) choices["Return to camp"] = ::endRun
        }
        when(event) {
            Event.GOOSE_WAS_NEVER_AN_OPTION -> {
                choices["RUN!"] = ::endRun
                return@createMemo choices
            }
            Event.WOODPILE -> {
                if(stamina>=2) {
                    choices["Pick up all the logs (1 Wood per 0.5 Stamina)"] = ::collectWoodPile
                }
            }
            Event.RAVEN -> {
                if(!crowFed) game.foods.filter{ (inventory[it.value.resource] ?: 0f) > 0f }.forEach {
                    choices["Feed the Crow (1 ${it.key})"] = { feedCrow(it) }
                }
            }
            Event.NONE -> {
                if(!groundSearched && stamina>=0.5f && inventory.values.sum()<inventoryMax()) {
                    choices["Search forest ground (0.5 stamina)"] = ::searchGround
                }
            }
        }
        if(stamina >= 1) {
            choices["Go further (1 Stamina)"] = ::doStep
        } else {
            game.foods.filter{ (inventory[it.value.resource] ?: 0f) > 0f }.forEach {
                choices["Eat 1 ${it.key} (+${it.value.nutrition} Stamina)"] = { eat(it.value.resource) }
            }
        }
        choices
    }


    div(`class` = { "flex flex-row justify-between" }) {
        span(`class` = { "text-3xl" }) { +{ zone.displayName } }
        Button(onClick = { endRun() }, `class` = { "bg-red-200" }) {
            +"Run back home"
        }
    }
    div(`class` = { "border rounded bg-gray-50 text-md p-2" }) {
        TypingAnimation(speed={ 6 }) { dialogText }
    }
    div(`class` = { "flex flex-row gap-2" }) {
        div(`class` = { "flex flex-col gap-1 grow" }) {
            span(`class` = { "text-xl" }) { +"What will you do?" }
            For({ playerChoices }) { key, value ->
                Button(onClick = { value() }) { +key }
            }
        }
        div(`class` = { "border-l" }) {}
        div(`class` = { "flex flex-col gap-1 grow" }) {
            div(`class` = { "flex flex-row text-xl gap-1 justify-between" }) {
                TextTooltip(text = { "Each step you take requires 1 Stamina. Eat food to replenish. When stamina runs low you return back to camp" }) {
                    +"Stamina"
                }
                ProgressBar(value = { stamina }, max = { 100 }, displayText = { false })
            }
            div(`class` = { "flex flex-row text-xl gap-1 justify-between" }) {
                +"Backpack"
                ProgressBar(value = { inventory.values.sum() }, max = inventoryMax, displayText = { false })
            }
            div(`class` = { "border border-gray-200 p-2" }) {
                span(`class` = { "text-xl" }) { +"Food" }
                For(inventory) { res, value ->
                    If({ game.foods.values.any { it.resource == res } }) {
                        div(`class` = { "ml-2 flex flex-row justify-between" }) {
                            Button(onClick = { eat(res) }) { +"Eat" }
                            span { +res.name }
                            span { +{ value().toInt().toString() } }
                        }
                    }
                }
            }
            For(inventory) { res, value ->
                If({ game.foods.values.none { it.resource == res } }) {
                    div(`class` = { "flex flex-row justify-between" }) {
                        span { +res.name }
                        span { +{ value().toInt().toString() } }
                    }
                }
            }
        }
    }
}