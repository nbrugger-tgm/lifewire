package eu.niton.ktx.spa.example

import eu.niton.ktx.spa.For
import eu.niton.ktx.spa.If
import eu.niton.ktx.spa.component
import eu.niton.ktx.spa.createSignal
import eu.niton.ktx.spa.cx
import eu.niton.ktx.spa.document
import eu.niton.ktx.spa.insert
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
import eu.niton.ktx.tags.script
import eu.niton.ktx.tags.span
import eu.nitonfx.signaling.api.ListSignal

fun main() {
    val mountPoint = document.getElementById("app")
    cx.run {
        insert({ render(BodyContent::App) }, mountPoint)
    }
}

class Task(name: String) {
    var name by createSignal(name)
    var done by createSignal(false)
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
        TodoCreator(onAdd = { t, i -> tasks.add(i, t) })
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
            If(task::done) {
                b(`class` = { "color-green mr-2" }) { +"done" }
            }
            +{ task.name }
            button(`class` = { "bg-green-200 rounded border-1 p-1" }, onClick = { task.done =! task.done }) {
                +{ if (!task.done) "Done" else "Still todo" }
            }
            button(`class` = { "bg-red-200 rounded border-1 p-1" }, onClick = { remove() }) {
                +"Delete"
            }
        }
    }
}

fun DivHtmlTag<*>.TodoCreator(onAdd: (Task, Int) -> Unit) = component {
    var nextTask by createSignal<String?>("")
    var index by createSignal(0)
    fun addTask() {
        if (nextTask == null) return
        val task = Task(nextTask!!)
        onAdd(task, index)
        nextTask = ""
    }
    div {
        span { +"Neuen Task anlegen" }
        input(
            `class` = { "border-1 rounded p-1 bg-gray-200" },
            placeholder = { "Task" },
            onInput = { nextTask = it },
            value = { nextTask }
        )
        input(
            `class` = { "border-1 rounded p-1 bg-gray-200" },
            placeholder = { "index" },
            onInput = { index = it?.toIntOrNull() ?: 0 },
            value = { index.toString() }
        )
        button(`class` = { "border-1 rounded p-1" }, onClick = { addTask() }) {
            +"Add"
        }
    }
}