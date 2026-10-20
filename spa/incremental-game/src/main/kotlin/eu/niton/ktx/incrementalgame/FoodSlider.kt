package eu.niton.ktx.incrementalgame

import eu.niton.ktx.spa.component
import eu.niton.ktx.tags.DivContent
import eu.niton.ktx.tags.DivHtmlTag
import eu.niton.ktx.tags.div
import eu.niton.ktx.tags.span
import eu.nitonfx.signaling.api.MapSignal

fun DivContent.FoodSlider(
    name: ()->String,
    inventory: MapSignal<Resource, Float>,
    food: Food,
    inventoryHasSpace: ()->Boolean
) = component {
    fun addFood() {
        if (inventoryHasSpace() && ((inventory[food.resource] ?: 0f) < food.resource.value))
            inventory[food.resource] = inventory[food.resource]?.plus(1f) ?: 1f
    }

    fun removeFood() {
        inventory[food.resource] = inventory[food.resource]?.minus(1f) ?: 0f
    }

    div(`class` = { "items-center" }) {
        +name
    }
    div(`class` = { "flex flex-row gap-1" }) {
        span(
            `class` = { "border text-sm px-1 bg-gray-100 cursor-pointer" },
            onClick = { removeFood() }
        ) { +"-" }
        ProgressBar(
            value = { inventory[food.resource] ?: 0f },
            max = { food.resource.value.toInt() },
            displayText = { false },
            insideText = { (inventory[food.resource] ?: 0).toString() },
            color = { "bg-orange-700" }
        )
        span(
            `class` = { "border text-sm px-1 bg-gray-100 cursor-pointer" },
            onClick = { addFood() }
        ) { +"+" }
    }
}