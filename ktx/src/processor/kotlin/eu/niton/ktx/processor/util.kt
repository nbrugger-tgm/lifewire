package eu.niton.ktx.processor

import java.net.URL
import java.util.*

private class ResourceLoader
private val locale = Locale.getDefault()

fun String.capitalize() = replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }

fun String.decapitalize() = replaceFirstChar { it.lowercase(locale) }

fun String.asResourceUrl(): URL = ResourceLoader::class.java.classLoader.getResource(this)
    ?: throw IllegalArgumentException("Resource $this not found in classpath.")

fun String.quote() = "\"$this\""

val reservedNames = setOf("class", "val", "var", "object", "true", "false", "as", "is", "for")
fun String.replaceIfReserved(): String {
    return when {
        this in reservedNames -> "html" + this.capitalize()
        this.contains('-') -> "`$this`" // Wrap hyphenated names in backticks
        else -> this
    }
}