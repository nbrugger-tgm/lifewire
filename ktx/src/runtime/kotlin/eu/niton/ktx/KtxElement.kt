package eu.niton.ktx
sealed interface KtxElement {
    data class Tag(
        val tag: kotlin.String,
        val body: KtxElement?,
        val attributes: Map<kotlin.String, (() -> kotlin.String?)?>,
        val eventListeners: Map<kotlin.String, ((kotlin.String?) -> Unit)?>
    ) : KtxElement
    data class String(val string: kotlin.String) : KtxElement
    data class Function(val content: () -> KtxElement?) : KtxElement
    data class List(val elements: kotlin.collections.List<KtxElement?>) : KtxElement
}