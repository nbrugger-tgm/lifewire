package eu.niton.ktx.incrementalgame

import eu.niton.ktx.incrementalgame.utils.discovery
import eu.niton.ktx.incrementalgame.utils.localStorageBooleanSignal
import eu.niton.ktx.incrementalgame.utils.localStorageEnumSignal
import eu.niton.ktx.incrementalgame.utils.localStorageFloatSignal
import eu.niton.ktx.incrementalgame.utils.localStorageIntSignal
import eu.niton.ktx.incrementalgame.utils.localStorageSignal
import eu.niton.ktx.incrementalgame.utils.localStorageStringSignal
import kotlin.collections.set

class Game {
    val resources = mutableMapOf<String, Resource>()

    val fire = Resource("fire", 100)

    val metal = Resource("metal", 30)
    val dirt = Resource("dirt", 30)
    val steam = Resource("steam", 30)
    val water = Resource("water", 50)
    val heat = Resource("heat", 150)
    val wood = Resource("wood", 30, defaultValue = 25f)
    val clay = Resource("clay", 40)
    val mushrooms = Resource("mushrooms", 5, defaultValue = 1f)
    val berries = Resource("berries", 5, defaultValue = 1f)

    val foods = mapOf(
        "Mushrooms" to Food(mushrooms,2),
        "Berries" to Food(berries,1),
    )
    var exploring by localStorageBooleanSignal("exploring", false)

    val bowl = Upgrade("bowl", mapOf(metal to 1f))
    val bucket = Upgrade("bucket", mapOf(metal to 3f, heat to 20f, water to 10f))

    class Exploration {
        enum class Zone(val displayName: String) {
            FOREST("Forest"),
            ABANDONED_HUT("The Abandoned Hut"),
            MUD_PIT("The Mud Pit")
        }
        enum class Event {
            GOOSE_WAS_NEVER_AN_OPTION,
            WOODPILE,
            RAVEN,
            NONE
        }
        var event by localStorageEnumSignal("exploration_event", Event.NONE)
        var zone by localStorageEnumSignal("exploration_zone", Zone.FOREST)
        var dialogText by localStorageStringSignal("exploration_dialog_text", "You enter the forest")
        var stamina by localStorageFloatSignal("stamina", 0f)
        var preparing by localStorageBooleanSignal("exploration_prep", true)
    }
    val exploration = Exploration()

    var dialogText by localStorageSignal(
        "dialog",
        { it },
        { it ?: "You find a bonfire in the woods, it cold and dark. Ignite the flame and heat it up" })

    val discoveredWaterWell by discovery("water_well", condition = { heat.value >= 75 }) {
        dialogText =
            "As the boiler heats up you wonder if it still works. You wander around the Lit up forest and find a water well"
    }
    val discoveredSteamBoiler by discovery("steam_boiler", condition = { fire.value >= 90 }) {
        dialogText =
            "As the fire kindles you see your surroundings lit up. The Bonfire seems to be part of a machine. Atop the bonfire sits a simple water boiler"
    }
    val discoveredMine by discovery("mine", { steam.value > 25 }) {
        dialogText =
            "As the machine fills with steam once again and parts start moving you notice a rotating shaft, drilling into the ground"
    }
    val discoveredOreWashing by discovery("ore-washing", { dirt.value >= 5 }) {
        dialogText =
            "As you shovel the dirt to the side with your hands you find a little piece of metal in the dirt. \"Surely there must be more\" you think, as you start washing your dirt searching for metal"
        metal.value = 1f
    }
    val discoverForest by discovery("forest", { wood.value <= 3 }) {
        dialogText =
            "The wood of the bonfire runs low ...   Luckily you are in a forest and now that you have a light you can see around you and gather wood for your fire, close to the fire you notice a little mushroom colony - a good snack for adventuring!"
    }
}
private fun Game.Resource(name: String, max: Int,defaultValue:Float?=null): Resource {
    if(resources[name] != null) return resources[name]!!
    val resource = eu.niton.ktx.incrementalgame.Resource(name, max, defaultValue)
    resources[name] = resource
    return resource
}