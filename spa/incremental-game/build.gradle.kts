plugins {
    kotlin("jvm")
    id("org.teavm") version "0.12.3"
}



dependencies {
    implementation(project(":spa"))
}

teavm {
    all {
        mainClass = "eu.niton.ktx.incrementalgame.ApplicationKt"
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
    args("-d", webAppDir.get(), "-b", "0.0.0.0")
}

repositories {
    mavenCentral()
}