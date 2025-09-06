plugins {
    kotlin("jvm")
    id("org.teavm") version "0.12.3"
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
    }
}
val distWebapp = tasks.register<Sync>("distWebapp") {
    from(tasks.generateJavaScript)
    from(sourceSets.main.get().resources)
    into(layout.buildDirectory.dir("dist/webapp"))
}
tasks.assemble { dependsOn(distWebapp) }

repositories {
    mavenCentral()
}