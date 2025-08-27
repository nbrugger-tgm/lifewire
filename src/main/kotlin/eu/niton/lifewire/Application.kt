package eu.niton.lifewire

import eu.niton.ktx.Content
import eu.niton.ktx.example.BodyContent
import eu.niton.ktx.example.DivContent
import eu.niton.ktx.example.DivHtmlTag
import eu.niton.ktx.example.div
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
    div {
        h1 {
            +"TODO List"
        }
        h3 {
            +{ tasks.size.toString() + " Tasks" }
        }
        TodoCreator(cx, onAdd = { tasks.add(it) })
        TaskList(tasks)
    }
}

fun DivContent.TaskList(tasks: ListSignal<Task>) = component {
    ol {
        For(elements = tasks) {
            li { TaskView(task = it, remove = { tasks.remove(it) }) }
        }
    }
}

fun DivContent.TaskView(task: Task, remove: () -> Unit) = component {
    If(task.done::get) {
        b { +"done" }
    }
    +{ task.name() }
    button(onClick = { task.done { !it } }) {
        +{ if (task.done()) "Done" else "Still todo" }
    }
    button(onClick = { remove() }) {
        +"Delete"
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
        input(onInput = { nextTask(it) })
        button(onClick = { addTask() }) {
            +"Add"
        }
    }
}