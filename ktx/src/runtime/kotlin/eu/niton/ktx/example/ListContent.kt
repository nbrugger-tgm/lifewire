package eu.niton.ktx.example

import eu.niton.ktx.ContentDefinition
import eu.niton.ktx.Content
import eu.niton.ktx.KtxElement
import eu.niton.ktx.render
import eu.niton.ktx.tag

interface ListContentDefinition<T> : ContentDefinition<T> where T: ListContentDefinition<T>, T: Content<T> {
    fun li(body: DivContent.() -> Unit) = tag("li", mapOf(), mapOf(), render(body))
}
class ListContent : Content<ListContent>(), ListContentDefinition<ListContent> {
    override fun createSubContent(): ListContent = ListContent()
}
inline fun render(body: ListContent.() -> Unit): KtxElement? = render(::ListContent, body)