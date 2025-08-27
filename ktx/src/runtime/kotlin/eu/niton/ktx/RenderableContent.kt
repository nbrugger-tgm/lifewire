package eu.niton.ktx

abstract class RenderableContent {
    @PublishedApi
    internal abstract fun toKtxElement(): KtxElement?
}
inline fun <C : RenderableContent> render(ctor: () -> C, body: C.() -> Unit): KtxElement? {
    return ctor().apply(body).toKtxElement()
}