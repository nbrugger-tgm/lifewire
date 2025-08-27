package eu.niton.lifewire

import eu.niton.lifewire.ktx.KTX
import eu.nitonfx.signaling.api.Context
import io.micronaut.scheduling.annotation.Async
import io.micronaut.websocket.WebSocketSession
import io.micronaut.websocket.annotation.OnClose
import io.micronaut.websocket.annotation.OnMessage
import io.micronaut.websocket.annotation.OnOpen
import io.micronaut.websocket.annotation.ServerWebSocket
import kotlin.collections.set

@ServerWebSocket("/wire")
open class WebSocketServer {
    val sessions = mutableMapOf<String, KTX>()
    @OnOpen
    fun open(session: WebSocketSession) {
       start(session)
    }

    @Async
    open fun start(session: WebSocketSession){
        val context = Context.create()
        val ktx = KTX(Wire.Minified(session), context)
        sessions[session.id] = ktx
        ktx.run(mainComponent)
    }

    @OnMessage
    fun webEvent(session: WebSocketSession, msg: String){
        val splitPoint = msg.indexOfFirst{ it == '$' }
        if(-1 == splitPoint) sessions[session.id]?.onEvent(msg.toLong(),null)
        else sessions[session.id]?.onEvent(msg.take(splitPoint).toLong(), msg.substring(splitPoint+1))
    }

    @OnClose
    fun onClose(session: WebSocketSession) {
        sessions.remove(session.id)
    }
}

