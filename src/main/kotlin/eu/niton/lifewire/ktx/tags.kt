package eu.niton.lifewire.ktx

import eu.niton.ktx.Content
import eu.niton.ktx.KtxElement
import eu.niton.ktx.RenderableContent
import eu.niton.ktx.render
import eu.nitonfx.signaling.api.ListSignal

fun interface ElseFn<T : RenderableContent> {
    fun Else(body: T.() -> Unit)
}

inline fun <T : Content<I>, I : RenderableContent> T.If(
    crossinline condition: () -> Boolean,
    noinline body: I.() -> Unit
): ElseFn<I> {
    var `else`: (I.() -> Unit)? = null
    +KtxElement.Function {
        if (condition()) render(this, body)
        else `else`?.let { render(this, it) }
    }
    return ElseFn { `else` = it }
}

inline fun <C : Content<T>, T : RenderableContent, E> C.For(elements: ListSignal<E>, crossinline body: T.(E) -> Unit) {
    +KtxElement.List(
        elements.map { element ->
            render(this) { body(element) }
        }
    )
}

inline fun <C : Content<T>, T : RenderableContent, E> C.For(
    crossinline elements: () -> List<E>,
    crossinline body: T.(E) -> Unit
) {
    +KtxElement.Function {
        KtxElement.List(
            elements().map { element ->
                render(this) { body(element) }
            }
        )
    }
}

inline fun <C : Content<T>, T : RenderableContent> C.component(crossinline body: T.() -> Unit) {
    +KtxElement.Function { render(this, body) }
}
