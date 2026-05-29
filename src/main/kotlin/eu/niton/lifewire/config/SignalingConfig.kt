package eu.niton.lifewire.config

import eu.nitonfx.signaling.api.Context
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Secondary
import java.util.function.Supplier

@Factory
class SignalingConfig {
    @Bean
    @Secondary
    fun contextFactory(): Supplier<Context> {
        return Supplier { Context.create() }
    }
}