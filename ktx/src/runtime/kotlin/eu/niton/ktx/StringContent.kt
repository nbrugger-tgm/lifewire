package eu.niton.ktx

interface StringContent<T> : ContentDefinition<T> where T: ContentDefinition<T>, T: Content<T> {
    operator fun String.unaryPlus() {
        +(KtxElement.String(this))
    }
    operator fun (()->String?).unaryPlus() {
        +(KtxElement.Function {
            this()?.let { KtxElement.String(it) }
        })
    }
}