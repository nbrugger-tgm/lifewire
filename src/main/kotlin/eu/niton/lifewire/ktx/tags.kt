package eu.niton.lifewire.ktx

import eu.niton.ktx.ContentDefinition
import eu.niton.ktx.Content
import eu.niton.ktx.KtxElement
import eu.niton.ktx.render
import eu.nitonfx.signaling.api.ListSignal
import java.util.function.Function
import kotlin.jvm.functions.FunctionN

fun interface ElseFn<T : ContentDefinition<*>> {
    fun Else(body: T.() -> Unit)
}

inline fun <T : Content<T>> T.If(crossinline condition: () -> Boolean, noinline body: T.() -> Unit): ElseFn<T> {
    var `else`: (T.() -> Unit)? = null
    +(KtxElement.Function {
        if (condition()) render(this, body)
        else `else`?.let { render(this, it) }
    })
    return ElseFn { `else` = it }
}

inline fun <T: Content<T>,E> T.For(elements: ListSignal<E>, crossinline body: T.(E) -> Unit) {
    +(KtxElement.List(elements.map { element -> render(this) { body(element) } }))
}
inline fun <T: Content<T>,E> T.For(crossinline elements: ()->List<E>, crossinline body: T.(E) -> Unit) {
    +(KtxElement.Function {
        KtxElement.List(elements().map { element -> render(this) { body(element) } })
    })
}

inline fun <T : Content<T>> ContentDefinition<T>.component(crossinline body: T.()->Unit) {
    +(KtxElement.Function{ render(this, body) })
}
