plugins {
    `java-library`
    id("org.teavm") version "0.12.3"
    kotlin("jvm")
}

dependencies {
    implementation("eu.niton.ktx:html5")
    implementation("eu.nitonfx.signaling:lib")
    implementation(teavm.libs.jsoApis)
}

teavm {
    all {
        mainClass = "eu.niton.ktx.spa.ApplicationKt"
    }
    wasmGC {
        addedToWebApp = true
    }
    js {
        addedToWebApp = true
        sourceMap = true
    }
}
tasks.assemble { dependsOn(tasks.generateJavaScript) }
tasks.assemble { dependsOn(tasks.generateWasmGC) }
tasks.assemble { dependsOn(tasks.copyWasmGCRuntime) }

repositories {
    mavenCentral()
}