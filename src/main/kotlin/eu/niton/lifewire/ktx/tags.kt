@file:GenerateKtx(Event::class)
package eu.niton.lifewire.ktx

import eu.niton.ktx.Content
import eu.niton.ktx.KtxElement
import eu.niton.ktx.RenderableContent
import eu.niton.ktx.annotation.GenerateKtx
import eu.niton.ktx.render
import eu.nitonfx.signaling.api.ListSignal

fun interface ElseFn<T : RenderableContent> {
    fun Else(body: T.() -> Unit)
}

inline fun <T : Content<I>, I> T.If(
    crossinline condition: () -> Boolean,
    crossinline body: I.() -> Unit
): ElseFn<I> where I : Content<I>, I : RenderableContent {
    var `else`: (I.() -> Unit)? = null
    +KtxElement.Function {
        if (condition()) render(this, body)
        else `else`?.let { render(this, it) }
    }
    return ElseFn { `else` = it }
}

inline fun <C : Content<T>, T, E> C.For(
    elements: ListSignal<E>,
    crossinline body: T.(E) -> Unit
) where T : Content<T>, T : RenderableContent {
    +KtxElement.List(
        elements.map { element ->
            render(this) { body(element) }
        }
    )
}

inline fun <C : Content<T>, T, E> C.For(
    crossinline elements: () -> List<E>,
    crossinline body: T.(E) -> Unit
) where T : Content<T>, T : RenderableContent {
    +KtxElement.Function {
        KtxElement.List(
            elements().map { element ->
                render(this) { body(element) }
            }
        )
    }
}

inline fun <C : Content<T>, T> C.component(crossinline body: T.() -> Unit) where T : Content<T>, T : RenderableContent {
    +KtxElement.Function { render(this, body) }
}
