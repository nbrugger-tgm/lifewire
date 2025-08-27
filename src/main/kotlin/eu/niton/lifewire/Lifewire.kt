package eu.niton.lifewire

import eu.niton.ktx.tags.BodyContent
import eu.niton.ktx.tags.content.FlowElementContent
import eu.nitonfx.signaling.api.Context
import io.micronaut.runtime.Micronaut

typealias MainComponent = BodyContent.(cx: Context) -> Unit
internal lateinit var mainComponent: MainComponent
fun runLifewire(args: Array<String>, ktx: MainComponent) {
    mainComponent = ktx
    Micronaut.run(*args)
}

