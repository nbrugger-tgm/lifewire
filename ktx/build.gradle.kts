import org.gradle.kotlin.dsl.application

plugins {
    `java-library`
    kotlin("jvm")
    id("com.google.devtools.ksp") version "2.2.0-2.0.2"
}

repositories {
    mavenCentral()
}

val codegenSources = sourceSets.register("processor") {
}
val runtimeSources = sourceSets.register("runtime") {
}

dependencies {
    add(codegenSources.get().implementationConfigurationName, "com.google.devtools.ksp:symbol-processing-api:2.2.0-2.0.2")
    add(codegenSources.get().implementationConfigurationName, "com.sun.xsom:xsom:20140925")
    add(codegenSources.get().implementationConfigurationName, "com.squareup:kotlinpoet:1.15.3")
    add(codegenSources.get().implementationConfigurationName, runtimeSources.map { it.runtimeClasspath })

    ksp(codegenSources.map { it.runtimeClasspath })
    compileOnly(codegenSources.map { it.output })
    // implementation(kotlin("stdlib-jdk8")) // Not needed with Kotlin JVM toolchain usually
    api(runtimeSources.map { it.runtimeClasspath })
}

kotlin {
    jvmToolchain(17)
}