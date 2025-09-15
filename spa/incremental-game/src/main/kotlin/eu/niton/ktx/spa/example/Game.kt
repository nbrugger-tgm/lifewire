package eu.niton.ktx.spa.example

import eu.niton.ktx.spa.example.utils.discovery
import eu.niton.ktx.spa.example.utils.localStorageBooleanSignal
import eu.niton.ktx.spa.example.utils.localStorageSignal

class Game {
    var exploring by localStorageBooleanSignal("exploring",false)

    val fire = Resource("fire", 100)
    val metal = Resource("metal", 30)
    val dirt = Resource("dirt", 30)
    val steam = Resource("steam", 30)
    val water = Resource("water", 50)
    val heat = Resource("heat", 150)
    val wood = Resource("wood", 30, defaultValue = 15f)

    var dialogText by localStorageSignal(
        "dialog",
        { it },
        { it ?: "You find a bonfire in the woods, it cold and dark. Ignite the flame and heat it up" })

    val bowl = Upgrade("bowl", mapOf(metal to 1f))
    val bucket = Upgrade("bucket", mapOf(metal to 3f, heat to 20f, water to 10f))

    val discoveredWaterWell by discovery("water_well", condition = { heat.value >= 75 }) {
        dialogText =
            "As the boiler heats up you wonder if it still works. You wander around the Lit up forest and find a water well"
    }
    val discoveredSteamBoiler by discovery("steam_boiler", condition = { fire.value >= 90 }) {
        dialogText =
            "As the fire kindles you see your surroundings lit up. The Bonfire seems to be part of a machine. Atop the bonfire sits a simple water boiler"
    }
    val discoveredMine by discovery("mine", { steam.value > 30 }) {
        dialogText =
            "As the machine fills with steam once again and parts start moving you notice a rotating shaft, drilling into the ground"
    }
    val discoveredOreWashing by discovery("ore-washing", { dirt.value >= 5 }) {
        dialogText =
            "As you shovel the dirt to the side with your hands you find a little piece of metal in the dirt. \"Surely there must be more\" you think, as you start washing your dirt searching for metal"
        metal.value = 1f
    }
    val discoverForest by discovery("forest", {wood.value<=3}) {
        dialogText = "The wood of the bonfire runs low ...   Luckily you are in a forest and now that you have a light you can see around you and gather wood for your fire, close to the fire you notice a little mushroom colony - a good snack for adventuring!"
    }
}