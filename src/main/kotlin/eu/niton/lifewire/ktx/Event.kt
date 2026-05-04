package eu.niton.lifewire.ktx

sealed interface Event {
    object Default : Event
    data class Input(val value: String) : Event
}