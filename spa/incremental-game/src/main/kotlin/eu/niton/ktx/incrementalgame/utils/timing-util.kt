package eu.niton.ktx.incrementalgame.utils

import eu.niton.ktx.spa.createSignal
import eu.niton.ktx.spa.cx
import org.teavm.jso.browser.Window
import java.util.function.Supplier
import kotlin.time.Duration
import kotlin.time.Duration.Companion.microseconds

private val MIN_WAIT = 50.microseconds
class Task(private val duration: () -> Duration,private val function: () -> Unit) {
    var percentDone by createSignal(0f)
        private set
    var inProgress by createSignal(false)
        private set
    fun perform() {
        if (cx.untracked(Supplier { inProgress })) return
        inProgress = true
        val initialDur = cx.untracked(Supplier { duration() })
        val delay = (initialDur / 100).coerceAtLeast(MIN_WAIT)
        var id = 0;
        id = Window.setInterval({
            percentDone += (delay/initialDur).toFloat()
            if (percentDone >= 1) {
                percentDone -= 1
                inProgress = false
                function()
                Window.clearInterval(id)
            }
        }, delay.inWholeMilliseconds.toInt())
    }
}
class Process(private val duration: () -> Duration, private val function: Process.(delta: Float) -> Unit) {
    private var timeout:Int? = null;
    private var running by createSignal(false)
    val isRunning get() = running
    init {
        cx.cleanup { stop() }
    }

    fun start() {
        if(timeout != null) return;
        startTimeout()
        running = true
    }
    private val intervall = cx.createMemo {
        val duration = this.duration()
        val delay = (duration/100).coerceAtLeast(MIN_WAIT)
        val multi = (delay/duration).toFloat()
        delay to multi
    }

    private fun startTimeout() {
        val (delay, multi) = cx.untracked(intervall::get)
        timeout = Window.setTimeout({ function(multi); if(timeout != null) startTimeout() }, delay.inWholeMilliseconds.toInt())
    }

    fun stop() {
        timeout?.let { Window.clearTimeout(it) }
        timeout = null
        running = false
    }
}