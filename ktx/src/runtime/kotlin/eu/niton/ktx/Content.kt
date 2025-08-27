package eu.niton.ktx

@DslMarker
private annotation class JsxDsl

@JsxDsl
interface Content<out THIS> where THIS : Content<THIS>, THIS : RenderableContent {
    operator fun KtxElement?.unaryPlus()
    fun createSubContent(): THIS
}

open class DefaultContent<out THIS>(private val ctor: () -> THIS) :
    RenderableContent(), Content<THIS>
        where THIS : RenderableContent,
              THIS : Content<THIS> {
    private val children = mutableListOf<KtxElement?>()

    override fun toKtxElement(): KtxElement? {
        return if (children.isEmpty()) null
        else if (children.size == 1) children[0]
        else KtxElement.List(children)
    }

    override fun KtxElement?.unaryPlus() {
        children.add(this)
    }

    override fun createSubContent(): THIS = ctor()
}

/**
 * Renders in a sub-content of the same type
 */
inline fun <T : Content<I>, I : RenderableContent> render(parent: T, body: I.() -> Unit): KtxElement? {
    return render(parent::createSubContent, body)
}

fun Content<*>.tag(
    name: String,
    attributes: Map<String, (() -> String?)?>,
    eventHandlers: Map<String, ((String?) -> Unit)?>,
    body: KtxElement?
) {
    +(KtxElement.Tag(name, body, attributes, eventHandlers))
}