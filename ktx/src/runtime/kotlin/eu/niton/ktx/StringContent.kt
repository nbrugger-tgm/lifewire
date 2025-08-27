package eu.niton.ktx

interface StringContent<T> : Content<T> where T: Content<T>, T: RenderableContent {
    operator fun String.unaryPlus() {
        +(KtxElement.String(this))
    }
    operator fun (()->String?).unaryPlus() {
        +(KtxElement.Function {
            this()?.let { KtxElement.String(it) }
        })
    }
}
@PublishedApi
internal class StringContentImpl : DefaultContent<StringContentImpl>(::StringContentImpl), StringContent<StringContentImpl>
inline fun render(body: StringContent<*>.() -> Unit): KtxElement? = render(::StringContentImpl, body)