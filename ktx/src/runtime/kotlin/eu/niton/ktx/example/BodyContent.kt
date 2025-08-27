package eu.niton.ktx.example

import eu.niton.ktx.ContentDefinition
import eu.niton.ktx.Content
import eu.niton.ktx.KtxElement
import eu.niton.ktx.tag
import eu.niton.ktx.render

//This should be in a separate file DivHtmlTag
interface DivHtmlTag<T> : ContentDefinition<T> where T: DivHtmlTag<T>, T: Content<T>
inline fun DivHtmlTag<*>.div(
    noinline `class`: (()->String)? = null,
    body: DivContent.() -> Unit
) = tag("div", mapOf("class" to `class`), mapOf(), render(body))

//This should be in a file BodyContent
internal interface BodyContentDefinition<T> : ContentDefinition<T>, DivHtmlTag<T> where T: DivHtmlTag<T>, T: Content<T>
class BodyContent : Content<BodyContent>(), BodyContentDefinition<BodyContent> {
    override fun createSubContent(): BodyContent = BodyContent()
}
inline fun render(body: BodyContent.() -> Unit): KtxElement? = render(::BodyContent, body)