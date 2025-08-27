package eu.niton.ktx

interface KtxRuntime<T : Tag> {
    fun createElement(tag: String):T
    fun insert(parent: T, child: T)
}