package eu.niton.ktx

abstract class Content<THIS> : ContentDefinition<THIS>
        where THIS : Content<THIS>,
              THIS : ContentDefinition<THIS> {
    private val children = mutableListOf<KtxElement?>()

    @PublishedApi
    internal fun toKtxElement(): KtxElement? {
        return if (children.isEmpty()) null
        else if (children.size == 1) children[0]
        else KtxElement.List(children)
    }

    override fun KtxElement?.unaryPlus() {
        children.add(this)
    }
}


inline fun <C: Content<*>> render(ctor: () -> C, body: C.() -> Unit): KtxElement?  {
    return ctor().apply(body).toKtxElement()
}



/**
 * Renders in a sub-content of the same type
 */
inline fun <T : ContentDefinition<I>, I:Content<I>> render(parent: T, body: I.() -> Unit): KtxElement? {
    return render(parent::createSubContent, body)
}