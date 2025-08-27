package eu.niton.lifewire

import eu.niton.lifewire.ktx.KTX
import io.micronaut.websocket.WebSocketSession

interface Wire {

    fun insertTag(tag: String, id: Long, parent: Long, slot: Long, offset: Long?)
    fun insertText(text: String, id: Long, parent: Long, slot: Long, offset: Long?)
    fun remove(id: Long)
    fun setAttribute(tag: Long, key: String, value: String?)
    fun setHandler(tag: Long, key: String, handlerId: Long)


    class Minified(private val session: WebSocketSession):Wire {
        override fun insertTag(tag: String, id: Long, parent: Long, slot: Long, offset: Long?) {
            val pos = offset?.let { "$slot,$it" } ?: slot.toString()
            session.sendAsync("${pos}+${parent}t${id}_${tag}")
        }

        override fun insertText(text: String, id: Long, parent: Long, slot: Long, offset: Long?) {
            val pos = offset?.let { "$slot,$it" } ?: slot.toString()
            session.sendAsync("${pos}+${parent}s${id}_${text}")
        }

        override fun remove(id: Long) {
            session.sendAsync("-${id}")
        }

        override fun setAttribute(tag: Long, key: String, value: String?) {
            if(value != null) session.sendAsync("a${tag}+$key=$value")
            else session.sendAsync("a${tag}-$key")
        }
        override fun setHandler(tag: Long, key: String, handlerId: Long) {
            session.sendAsync("a${tag}+$key=e($handlerId,event.target.value)")
        }
    }
}
