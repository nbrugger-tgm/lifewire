plugins {
    `java-library`
    kotlin("jvm")
}

dependencies {
    api("eu.niton.ktx:html5")
    api("eu.nitonfx.signaling:lib")
    api("org.teavm:teavm-jso-apis:0.12.3")
}

repositories {
    mavenCentral()
}