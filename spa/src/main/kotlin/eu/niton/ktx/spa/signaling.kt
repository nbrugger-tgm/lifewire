package eu.niton.ktx.spa

import eu.nitonfx.signaling.api.Context
import eu.nitonfx.signaling.api.Signal
import eu.nitonfx.signaling.api.SignalLike
import java.util.function.Supplier
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

val cx = Context.global
operator fun <T> SignalLike<T>.invoke(): T = get()
operator fun <T> Signal<T>.invoke(value: T) = set(value)
operator fun <T> Signal<T>.invoke(value: (T) -> T) = update(value)

class SignalProperty<T>(private var signal: Signal<T>) : ReadWriteProperty<Any?, T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return signal()
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        signal(value)
    }
}
class SignalLikeProperty<T>(private var signal: Supplier<T>) : ReadOnlyProperty<Any?, T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return signal.get()
    }
}

fun <T> createSignal(init: T): ReadWriteProperty<Any?, T> {
    return SignalProperty(cx.createSignal(init))
}
fun <T> createMemo(fn: () -> T): ReadOnlyProperty<Any?, T> {
    return SignalLikeProperty(cx.createMemo(fn))
}
fun <N> derived(fn: ()->N): ReadOnlyProperty<Any?,N> {
    return SignalLikeProperty(fn)
}

