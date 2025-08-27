# Lifewire

An experimental way to write browser frontend applications in kotlin with JVM support.

The idea is to be able to write React/SolidJS/JSX like application with the full power of the JVM,
where the application runs as a server that sends minified commands to the browser to manipulate the DOM.

## How does it look?
> A full working and runnable example can be found in [`Application.kt`](src/main/kotlin/eu/niton/lifewire/Application.kt)

```kotlin
fun main(args: Array<String>) {
    runLifewire(args) { cx -> App(cx) }
}

fun BodyContent.App(cx: Context) {
    val tasks = cx.createSignal(listOf(
        Task(cx, "Create Some tasks"),
        Task(cx, "And delete some others")
    ))
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
```
### How it works
Similar to SolidJS, everything is based on signals and fine-grained reactivity, nothing that doesn't need to be updated is.#
This is necessary to keep the amount of data sent to the browser as low as possible. When an IF block switches from true to false,
only a single "remove element" command is sent to the browser, instead of re-rendering the whole block.

The commands are sent down to the browser via a websocket in a minified format. The browser parses the commands and applies them to the DOM.

#### Example commands
| command         | meaning                                                                              |
|-----------------|--------------------------------------------------------------------------------------|
| 1+0t3_div       | Insert a div with id 3 as a child of element with id 0 into slot 1                   |
| 0+4s5_TODO List | Insert a text node with id 5 "TODO List" as a child of element with id 4 into slot 0 |
| a9+type=1       | Set attribute "type" to "1" on element with id 9                                     |


