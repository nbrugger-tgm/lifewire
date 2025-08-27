package eu.niton.ktx

@DslMarker
private annotation class JsxDsl

@JsxDsl
interface ContentDefinition<THIS> where THIS : ContentDefinition<THIS>, THIS : Content<THIS> {
    operator fun KtxElement?.unaryPlus()
    fun createSubContent(): THIS
}

fun ContentDefinition<*>.tag(
    name: String,
    attributes: Map<String, (() -> String?)?>,
    eventHandlers: Map<String, ((String?) -> Unit)?>,
    body: KtxElement?
) {
    +(KtxElement.Tag(name, body, attributes, eventHandlers))
}