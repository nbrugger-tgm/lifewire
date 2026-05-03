plugins {
    `java-library`
    kotlin("jvm")
    id("com.google.devtools.ksp")
}

dependencies {
    ksp("eu.niton.ktx:processor")
    compileOnly("eu.niton.ktx:annotations")
    api("eu.nitonfx.signaling:lib")
    api("org.teavm:teavm-jso-apis:0.12.3")
}

repositories {
    mavenCentral()
}