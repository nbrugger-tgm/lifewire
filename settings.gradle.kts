pluginManagement {
    plugins {
        kotlin("jvm") version "2.2.0"
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
includeBuild(file("./libs/signaling"))
include("ktx")
include("example")

rootProject.name="lifewire"

