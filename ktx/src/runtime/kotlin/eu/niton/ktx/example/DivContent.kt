package eu.niton.ktx.example

import eu.niton.ktx.ContentDefinition
import eu.niton.ktx.Content
import eu.niton.ktx.KtxElement
import eu.niton.ktx.StringContent
import eu.niton.ktx.tag
import eu.niton.ktx.render

internal interface DivContentDefinition<T> :
    ContentDefinition<T>,
    StringContent<T>,
    BodyContentDefinition<T>
where T: DivContentDefinition<T>, T: Content<T> {
    //Each of this tag functions should have its own interface + file (see DivHtmlTag) and added via interface inheritance
    fun button(onClick: ((String?) -> Unit)? = null, body: DivContent.() -> Unit) =
        tag("button", mapOf(), mapOf("onClick" to onClick), render(body))
    fun h3(body: DivContent.() -> Unit) = tag("h3", mapOf(), mapOf(), render(body))
    fun h1(body: DivContent.() -> Unit) = tag("h1", mapOf(), mapOf(), render(body))
    fun span(body: DivContent.() -> Unit) = tag("span", mapOf(), mapOf(), render(body))
    fun a(body: DivContent.() -> Unit) = tag("a", mapOf(), mapOf(), render(body))
    fun b(body: DivContent.() -> Unit) = tag("b", mapOf(), mapOf(), render(body))
    fun input(onInput: ((String?) -> Unit)? = null) = tag("input", mapOf(), mapOf("oninput" to onInput), null)
    fun ul(body: ListContent.() -> Unit) = tag("ul", mapOf(), mapOf(), render(body))
    fun ol(body: ListContent.() -> Unit) = tag("ol", mapOf(), mapOf(), render(body))
}
class DivContent : Content<DivContent>(), DivContentDefinition<DivContent> {
    override fun createSubContent(): DivContent = DivContent()
}
inline fun render(body: DivContent.() -> Unit): KtxElement? = render(::DivContent, body)