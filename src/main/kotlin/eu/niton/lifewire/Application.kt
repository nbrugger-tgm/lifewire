package eu.niton.lifewire

import eu.niton.ktx.tags.*
import eu.niton.lifewire.ktx.For
import eu.niton.lifewire.ktx.If
import eu.niton.lifewire.ktx.component
import eu.niton.lifewire.signaling.invoke
import eu.nitonfx.signaling.api.Context
import eu.nitonfx.signaling.api.ListSignal
import eu.nitonfx.signaling.api.Signal

fun main(args: Array<String>) {
    runLifewire(args) { cx ->
        App(cx)
    }
}

class Task(cx: Context, name: String) {
    val name: Signal<String> = cx.createSignal(name)
    val done: Signal<Boolean> = cx.createSignal(false)
}

fun BodyContent.App(cx: Context) {
    val tasks = cx.createSignal(listOf(
        Task(cx, "Create Some tasks"),
        Task(cx, "And delete some others")
    ))
    script(src= { "https://cdn.jsdelivr.net/npm/@tailwindcss/browser@4" }){}
    div(`class` = {"flex flex-col gap-4"}) {
        h1(`class` = {"text-3xl font-bold"}) {
            +"TODO List"
        }
        h3(`class` = {"text-xl color-gray-600 m-1"}) {
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
        div(`class` = {"flex flex-row gap-1"}) {
            If(task.done::get) {
                b(`class` = {"color-green mr-2"}) { +"done" }
            }
            +{ task.name() }
            button(`class` = {"bg-green-200 rounded border-1 p-1"}, onClick = { task.done { !it } }) {
                +{ if (!task.done()) "Done" else "Still todo" }
            }
            button(`class` = {"bg-red-200 rounded border-1 p-1"},onClick = { remove() }) {
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
        input(`class` = {"border-1 rounded p-1 bg-gray-200"},onInput = { nextTask(it) })
        button(`class` = {"border-1 rounded p-1"}, onClick = { addTask() }) {
            +"Add"
        }
    }
}