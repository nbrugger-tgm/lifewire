package eu.niton.lifewire

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Produces
import jakarta.inject.Singleton



@Singleton
@Controller
open class HtmlController {
    @Get
    @Produces(value = ["text/html"])
    fun getHtml(): String {
        val script = this::class.java.classLoader.getResource("client.js")?.readText()
        return "<html><head><script>$script</script></head><body id='0'></body></html>";
    }
}