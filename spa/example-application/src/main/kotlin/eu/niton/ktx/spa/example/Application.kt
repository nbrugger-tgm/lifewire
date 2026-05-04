package eu.niton.ktx.spa.example

import eu.niton.ktx.spa.*
import eu.niton.ktx.spa.tags.*
import eu.niton.ktx.spa.tags.content.render
import eu.nitonfx.signaling.api.ListSignal
import org.teavm.jso.dom.html.HTMLInputElement

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
            onInput = { nextTask = (it.target as HTMLInputElement).value },
            value = { nextTask }
        )
        input(
            `class` = { "border-1 rounded p-1 bg-gray-200" },
            placeholder = { "index" },
            onInput = { index = (it.target as HTMLInputElement).value.toIntOrNull() ?: 0 },
            value = { index.toString() }
        )
        button(`class` = { "border-1 rounded p-1" }, onClick = { addTask() }) {
            +"Add"
        }
    }
}