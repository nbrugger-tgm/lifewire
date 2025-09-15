plugins {
    kotlin("jvm")
    id("org.teavm") version "0.12.3"
}

val server by sourceSets.registering {
}

dependencies {
    implementation(project(":spa"))
}

teavm {
    all {
        mainClass = "eu.niton.ktx.spa.example.ApplicationKt"
    }
    js {
        addedToWebApp = true
        sourceMap = true
        obfuscated = false
    }
}
val webAppDir = layout.buildDirectory.dir("dist/webapp")
val distWebapp = tasks.register<Sync>("distWebapp") {
    from(tasks.generateJavaScript)
    from(sourceSets.main.get().resources)
    into(webAppDir)
}
tasks.assemble { dependsOn(distWebapp) }

tasks.register<JavaExec>("run") {
    mainModule = "jdk.httpserver"
    args("-d", webAppDir.get())

}

repositories {
    mavenCentral()
}