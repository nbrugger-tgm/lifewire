package eu.niton.lifewire.ktx

import eu.niton.ktx.KtxElement
import eu.niton.ktx.tags.BodyBody
import eu.niton.ktx.tags.content.render
import eu.niton.lifewire.MainComponent
import eu.niton.lifewire.Wire
import eu.nitonfx.signaling.api.Context
import eu.nitonfx.signaling.api.ListSignal
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class KTX(private val wire: Wire, private val cx: Context) {
    private val handlers = mutableMapOf<Long, (String?) -> Unit>()
    var handlerId = 0L
    var tagId = 0L

    fun onEvent(id: Long, content: String?) {
        handlers[id]?.invoke(content)
    }

    class Parent(val id: Long) {
        private var slots = 0L
        fun nextSlot(): Long = this.slots++
    }

    val scheduling: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    fun run(mainComponent: MainComponent) {
        cx.run {
            val body : BodyBody = { mainComponent(cx) }
            insert({ render(body) }, Parent(tagId++))
        }.let {
            scheduling.schedule({print(it.formatAsTree())},30, TimeUnit.SECONDS)
        }
    }

    fun insert(component: ()-> KtxElement?, parent: Parent, slot: Long? = null, id: Long? = null, offset: Long? = null) {
        val id = id ?: tagId++
        cx.createEffect {
            insertRaw(component(), parent, slot, id, offset)
        }.name("insert function($id) into ${parent.id}")
    }

    fun insert(component: KtxElement.List, parent: Parent, slot: Long? = null, id: Long? = null, offset: Long? = null) {
        if (offset != null && slot == null) throw IllegalArgumentException("Offset only allowed within slot")
        val list = component.elements
        if (list is ListSignal) {
            val slot = slot?: parent.nextSlot()
            list.onAdd { it, index ->
                insert(it::get, parent, slot = slot, offset = (offset ?: 0) + index, id = null)
            }.name("insert list($id) into ${parent.id}")
        } else {
            list.forEachIndexed { index, element ->
                if (slot == null) insertRaw(element, parent, slot = index.toLong(), id = null)
                else insertRaw(element, parent, slot = slot, offset = (offset ?: 0) + index, id = null)
            }
        }
    }


    private fun insertRaw(
        element: KtxElement?,
        parent: Parent,
        slot: Long? = null,
        id: Long? = null,
        offset: Long? = null
    ) {
        when (element) {
            is KtxElement.String -> insert(element, parent, slot, id, offset)
            is KtxElement.Function -> insert(element.content, parent, slot, id, offset)
            is KtxElement.List -> insert(element, parent, slot, id, offset)
            is KtxElement.Tag -> insert(element, parent, slot, id, offset)
            null -> insertNull(parent, slot, id, offset)
        }
    }

    fun insertNull(parent: Parent, slot: Long? = null, id: Long? = null, offset: Long? = null) {
        val slot = slot ?: parent.nextSlot()
        val id = id ?: tagId++
        wire.insertText("", id, parent.id, slot, offset)
        cx.cleanup {
            wire.remove(id)
        }
    }

    fun insert(
        component: KtxElement.String,
        parent: Parent,
        slot: Long? = null,
        id: Long? = null,
        offset: Long? = null
    ) {
        val slot = slot ?: parent.nextSlot()
        val id = id ?: tagId++
        wire.insertText(component.string, id, parent.id, slot, offset)
        cx.cleanup {
            wire.remove(id)
        }
    }

    fun insert(component: KtxElement.Tag, parent: Parent, slot: Long? = null, id: Long? = null, offset: Long? = null) {
        val slot = slot ?: parent.nextSlot()
        val id = id ?: tagId++
        wire.insertTag(component.tag, id, parent.id, slot, offset)
        cx.cleanup {
            wire.remove(id)
        }
        component.attributes.forEach {
            val value = it.value ?: return@forEach
            cx.createEffect {
                wire.setAttribute(id, it.key, value())
            }.name("set attribute ${it.key}")
        }
        component.eventListeners.forEach {
            val value = it.value ?: return@forEach
            val handler = handlerId++
            handlers[handler] = value
            wire.setHandler(id, it.key, handler)
            cx.cleanup {
                handlers.remove(handler)
            }
        }
        component.body?.let { insertRaw(element = it, parent = Parent(id)) }
    }
}