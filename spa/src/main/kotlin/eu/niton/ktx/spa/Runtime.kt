package eu.niton.ktx.spa

import eu.niton.ktx.KtxElement
import eu.nitonfx.signaling.api.ListSignal
import eu.nitonfx.signaling.api.SetSignal
import org.teavm.jso.dom.html.HTMLDocument
import org.teavm.jso.dom.html.HTMLElement
import org.teavm.jso.dom.html.HTMLInputElement
import org.teavm.jso.dom.xml.Node
import org.teavm.jso.dom.xml.Text

val document get() = HTMLDocument.current()
fun insert(function: () -> KtxElement?, parent: HTMLElement) {
    insert(function, parent, null)
}

private sealed interface Slot {
    data class Single(val node: Node) : Slot
    data class List(val nodes: kotlin.collections.List<Slot>) : Slot {
        init {
            if (nodes.isEmpty()) throw IllegalArgumentException("Nodes cannot be empty")
        }
    }

    class Function(fn: () -> Slot) : Function0<Slot> by fn, Slot
}

private fun insert(function: () -> KtxElement?, parent: HTMLElement, placeholder: Slot? = null): Slot {
    var placeholder = placeholder ?: insertNull(parent)
    fun plc() = placeholder
    cx.createEffect {
        placeholder = insertRaw(function(), parent, plc())
    }
    return Slot.Function(::plc)
}

private fun insertRaw(element: KtxElement?, parent: HTMLElement, placeholder: Slot? = null): Slot {
    return when (element) {
        is KtxElement.Function -> insert(element.content, parent, placeholder)
        is KtxElement.List -> insert(element.elements, parent, placeholder)
        is KtxElement.String -> insert(element.string, parent, placeholder)
        is KtxElement.Tag -> insert(element, parent, placeholder)
        null -> insertNull(parent, placeholder)
    }
}

private fun insert(string: KtxElement.Tag, parent: HTMLElement, placeholder: Slot? = null): Slot {
    val node = document.createElement(string.tag)
    if (placeholder != null) {
        replace(parent, placeholder, node)
    } else {
        parent.appendChild(node)
        cx.cleanup {
            parent.removeChild(node)
        }
    }
    string.attributes.forEach { attr ->
        val valueFn = attr.value
        if (valueFn != null) cx.createEffect {
            val value = valueFn()
            if (value != null) node.setAttribute(attr.key, value)
            else node.removeAttribute(attr.key)
        }
    }
    string.eventListeners.forEach { listener ->
        val func = listener.value
        if (func != null) {
            node.addEventListener(listener.key.replace("on", "")) {
                if (node is HTMLInputElement) func(node.value)
                else func(null)
            }
        }
    }
    insertRaw(string.body, node)
    return Slot.Single(node)
}

private fun insert(string: String, parent: HTMLElement, placeholder: Slot? = null): Slot {
    if (placeholder != null && placeholder is Slot.Single && placeholder.node is Text) {
        placeholder.node.textContent = string
        return placeholder
    }
    val node = document.createTextNode(string)
    replace(parent, placeholder, node)
    return Slot.Single(node)
}


private fun insert(string: Collection<KtxElement?>, parent: HTMLElement, placeholder: Slot?): Slot {
    if (string is ListSignal<KtxElement?>) {
        val placeholder = placeholder ?: insertNull(parent)
        string.onAdd { e, i ->
            val ktx = KtxElement.Function(e::get)
            val slot = insertAfter(parent, placeholder, ktx, offset = i)
            cx.cleanup { remove(parent, slot) }
        }
        return placeholder
    } else if (string is SetSignal<KtxElement?>) {
        val placeholder = placeholder ?: insertNull(parent)
        string.onAdd { ktx ->
            val slot = insertAfter(parent, placeholder, ktx)
            cx.cleanup { remove(parent, slot) }
        }
        return placeholder
    } else {
        if (string.isEmpty()) return placeholder ?: insertNull(parent)
        var previous = insertRaw(string.first(), parent, placeholder)
        val nodes = mutableListOf(previous)
        string.drop(1).forEach {
            previous = insertAfter(parent, previous, it)
            nodes.add(previous)
        }
        return Slot.List(nodes)
    }
}

private fun insertNull(parent: HTMLElement, placeholder: Slot? = null): Slot {
    if (placeholder != null && placeholder is Slot.Single && placeholder.node.nodeType == Node.TEXT_NODE) {
        placeholder.node.textContent = ""
        return placeholder
    }
    val node = document.createTextNode("")
    replace(parent, placeholder, node)
    return Slot.Single(node)
}

private fun replace(parent: HTMLElement, slot: Slot?, node: Node) {
    when (slot) {
        is Slot.Single -> parent.replaceChild(node, slot.node)
        is Slot.List -> {
            replace(parent, slot.nodes.first(), node)
            slot.nodes.drop(1).forEach { remove(parent, it) }
        }

        is Slot.Function -> replace(parent, slot(), node)
        null -> parent.appendChild(node)
    }
}

private fun insertAfter(parent: HTMLElement, slot: Slot?, node: Node, offset: Int = 0) {
    when (slot) {
        is Slot.Function -> insertAfter(parent, slot(), node, offset)
        is Slot.List -> insertAfter(parent, slot.nodes.last(), node, offset)
        is Slot.Single -> {
            var next = slot.node.nextSibling
            var offset = offset
            while (offset > 0 && next != null) {
                offset--
                next = next.nextSibling
            }
            if (next != null) parent.insertBefore(node, next)
            else parent.appendChild(node)
        }

        null -> parent.appendChild(node)
    }
}

private fun insertAfter(parent: HTMLElement, slot: Slot?, node: KtxElement?, offset: Int = 0): Slot {
    val placeholder = document.createTextNode("")
    insertAfter(parent, slot, placeholder, offset)
    return insertRaw(node, parent, Slot.Single(placeholder))
}

private fun remove(parent: HTMLElement, slot: Slot) {
    when (slot) {
        is Slot.Single -> parent.removeChild(slot.node)
        is Slot.List -> slot.nodes.forEach { remove(parent, it) }
        is Slot.Function -> remove(parent, slot())
    }
}