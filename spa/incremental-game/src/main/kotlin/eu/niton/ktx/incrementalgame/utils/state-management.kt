package eu.niton.ktx.incrementalgame.utils

import eu.niton.ktx.spa.SignalProperty
import eu.niton.ktx.spa.cx
import eu.niton.ktx.spa.invoke
import org.teavm.jso.browser.Window
import kotlin.enums.enumEntries
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty


inline fun <T> localStorageSignal(
    name: String,
    crossinline serialize: (T) -> String,
    deserialize: (String?) -> T
): ReadWriteProperty<Any?, T> {
    val signal = cx.createSignal(deserialize(Window.current().localStorage.getItem(name)))
    var saveDelay:Int? = null;
    var saveInterval:Int? = null;
    cx.createEffect {
        val newVal = signal()
        if(saveInterval == null) {
            saveInterval = Window.setInterval({
                if(saveDelay == null) return@setInterval
                Window.current().localStorage.setItem(name, serialize(signal()))
            },1000)
        }
        saveDelay = Window.setTimeout({
            Window.current().localStorage.setItem(name, serialize(newVal))
            if(saveInterval != null) {
                Window.clearInterval(saveInterval!!)
                @Suppress("AssignedValueIsNeverRead")
                saveInterval=null
            }
        }, 500)
        cx.cleanup {
            if(saveDelay == null) return@cleanup
            Window.clearTimeout(saveDelay!!)
            saveDelay = null
        }
    }
    return SignalProperty(signal)
}

fun localStorageIntSignal(name: String, default: Int): ReadWriteProperty<Any?, Int> {
    return localStorageSignal(name, Int::toString) {
        it?.toInt() ?: default
    }
}

fun localStorageStringSignal(name: String, default: String): ReadWriteProperty<Any?, String> {
    return localStorageSignal(name, { it }, { it ?: default })
}

inline fun <reified E: Enum<E>> localStorageEnumSignal(name: String, default: E): ReadWriteProperty<Any?, E> {
    return localStorageSignal(name, { it.name }, { str -> enumEntries<E>().find { it.name == str } ?: default })
}

fun localStorageFloatSignal(name: String, default: Float): ReadWriteProperty<Any?, Float> {
    return localStorageSignal(name, Float::toString) {
        it?.toFloat() ?: default
    }
}

fun localStorageBooleanSignal(name: String, default: Boolean): ReadWriteProperty<Any?, Boolean> {
    return localStorageSignal(name, Boolean::toString, { it?.toBoolean() ?: default })
}

fun discovery(name: String, condition: () -> Boolean, init: () -> Unit): ReadOnlyProperty<Any?, Boolean> {
    val discoveryProp = localStorageBooleanSignal("${name}_discovered", false)
    var discovery by discoveryProp
    cx.createEffect {
        if (discovery) return@createEffect
        if (condition()) {
            discovery = true
            init()
        }
    }
    return discoveryProp
}