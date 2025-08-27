package eu.niton.lifewire.signaling

import eu.nitonfx.signaling.api.Signal
import eu.nitonfx.signaling.api.SignalLike

operator fun <T> SignalLike<T>.invoke():T = get()
operator fun <T> Signal<T>.invoke(value: T) = set(value)
operator fun <T> Signal<T>.invoke(value: (T)->T) = update(value)