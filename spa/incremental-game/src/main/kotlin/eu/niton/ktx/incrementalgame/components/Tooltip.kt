package eu.niton.ktx.incrementalgame.components

import eu.niton.ktx.tags.SpanBody
import eu.niton.ktx.tags.SpanHtmlTag
import eu.niton.ktx.tags.span

inline fun SpanHtmlTag<*>.TextTooltip(
    noinline text: () -> String,
    noinline `class`: (() -> String)? = null,
    crossinline hover: SpanBody
) {
    Tooltip(tooltip = { +text }, `class` = `class`, hover = hover)
}

inline fun SpanHtmlTag<*>.Tooltip(
    crossinline tooltip: SpanBody,
    noinline `class`: (() -> String)? = null,
    crossinline hover: SpanBody
) {
    span(`class` = { "group relative ${`class`?.invoke()}" }) {
        hover()
        span(`class` = { "absolute transform left-1/2 -translate-x-1/2 w-max max-w-60 top-full mb-2 z-10 rounded border bg-gray-200 shadow-xl p-2 hidden group-hover:block" }) {
            tooltip()
        }
    }
}