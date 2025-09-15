package eu.niton.ktx.spa.example.utils

import eu.niton.ktx.spa.SignalProperty
import eu.niton.ktx.spa.cx
import eu.niton.ktx.spa.invoke
import org.teavm.jso.browser.Window
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty


inline fun <T> localStorageSignal(
    name: String,
    crossinline serialize: (T) -> String,
    deserialize: (String?) -> T
): ReadWriteProperty<Any?, T> {
    val signal = cx.createSignal(deserialize(Window.current().localStorage.getItem(name)))
    cx.createEffect {
        Window.current().localStorage.setItem(name, serialize(signal()))
    }
    return SignalProperty(signal)
}

fun localStorageIntSignal(name: String, default: Int): ReadWriteProperty<Any?, Int> {
    return localStorageSignal(name, Int::toString) {
        it?.let { it.toInt() } ?: default
    }
}
fun localStorageStringSignal(name: String, default: String): ReadWriteProperty<Any?, String> {
    return localStorageSignal(name, { it }, {it?: default})
}
fun localStorageFloatSignal(name: String, default: Float): ReadWriteProperty<Any?, Float> {
    return localStorageSignal(name, Float::toString) {
        it?.let { it.toFloat() } ?: default
    }
}

fun localStorageBooleanSignal(name: String, default: Boolean): ReadWriteProperty<Any?, Boolean> {
    return localStorageSignal(name, Boolean::toString, { it?.let { it.toBoolean() } ?: default })
}

fun discovery(name: String, condition: ()->Boolean, init: ()->Unit): ReadOnlyProperty<Any?,Boolean> {
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