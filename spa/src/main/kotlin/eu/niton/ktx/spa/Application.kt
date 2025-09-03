package eu.niton.ktx.spa

import eu.niton.ktx.tags.BodyContent
import eu.niton.ktx.tags.DivContent
import eu.niton.ktx.tags.DivHtmlTag
import eu.niton.ktx.tags.LiHtmlTag
import eu.niton.ktx.tags.b
import eu.niton.ktx.tags.button
import eu.niton.ktx.tags.content.render
import eu.niton.ktx.tags.div
import eu.niton.ktx.tags.h1
import eu.niton.ktx.tags.h3
import eu.niton.ktx.tags.input
import eu.niton.ktx.tags.li
import eu.niton.ktx.tags.ol
import eu.niton.ktx.tags.render
import eu.niton.ktx.tags.script
import eu.niton.ktx.tags.span
import eu.nitonfx.signaling.api.Context
import eu.nitonfx.signaling.api.ListSignal
import eu.nitonfx.signaling.api.Signal
import org.teavm.jso.dom.html.HTMLDocument

val cx = Context.global;

fun main() {
    val mountPoint = HTMLDocument.current().getElementById("app")
    insert({ render(BodyContent::App) }, mountPoint)
}

class Task(name: String) {
    val name: Signal<String> = cx.createSignal(name)
    val done: Signal<Boolean> = cx.createSignal(false)
}

fun BodyContent.App() {
    val tasks = cx.createSignal(
        listOf(
            Task("Create Some tasks"),
            Task("And delete some others")
        )
    )
    script(src = { "https://cdn.jsdelivr.net/npm/@tailwindcss/browser@4" }) {}
    div(`class` = { "flex flex-col gap-4" }) {
        h1(`class` = { "text-3xl font-bold" }) {
            +"TODO List"
        }
        h3(`class` = { "text-xl color-gray-600 m-1" }) {
            +{ tasks.size.toString() + " Tasks" }
        }
        TodoCreator(cx, onAdd = { tasks.add(it) })
        TaskList(tasks)
    }
}

fun DivContent.TaskList(tasks: ListSignal<Task>) = component {
    ol {
        For(elements = tasks) {
            TaskView(task = it, remove = { tasks.remove(it) })
        }
    }
}

fun LiHtmlTag<*>.TaskView(task: Task, remove: () -> Unit) = component {
    li {
        div(`class` = { "flex flex-row gap-1" }) {
            If(task.done::get) {
                b(`class` = { "color-green mr-2" }) { +"done" }
            }
            +{ task.name() }
            button(`class` = { "bg-green-200 rounded border-1 p-1" }, onClick = { task.done { !it } }) {
                +{ if (!task.done()) "Done" else "Still todo" }
            }
            button(`class` = { "bg-red-200 rounded border-1 p-1" }, onClick = { remove() }) {
                +"Delete"
            }
        }
    }
}

fun DivHtmlTag<*>.TodoCreator(cx: Context, onAdd: (Task) -> Unit) = component {
    val nextTask = cx.createSignal("")
    fun addTask() {
        if (nextTask() == null) return
        val task = Task(cx, nextTask())
        onAdd(task)
        nextTask(null)
    }
    div {
        span { +"Neuen Task anlegen" }
        input(`class` = { "border-1 rounded p-1 bg-gray-200" }, onInput = { nextTask(it) })
        button(`class` = { "border-1 rounded p-1" }, onClick = { addTask() }) {
            +"Add"
        }
    }
}