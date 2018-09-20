import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.plugin.source.KotlinSourceSet

group = "org.ice1000.ruiko"
version = "v0.1"

plugins {
	application
}

buildscript {
	val kotlinVersion = "1.3-M2"
	repositories { maven("http://dl.bintray.com/kotlin/kotlin-eap") }
	dependencies { classpath ("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion") }
}

apply {
	plugin("kotlin")
}

java.sourceSets {
	"main" {
		java.setSrcDirs(listOf("src"))
		withConvention(KotlinSourceSet::class) { kotlin.setSrcDirs(listOf("src")) }
		resources.setSrcDirs(listOf("res"))
	}
}

application.mainClassName = "$group.MainKt"

repositories {
	jcenter()
	maven("https://dl.bintray.com/ice1000/ice1000")
	maven("http://dl.bintray.com/kotlin/kotlin-eap")
}

dependencies {
	compile(kotlin("stdlib-jdk8"))
	compile("org.jetbrains.kotlinx:kotlinx-coroutines-core:0.25.3")
	val jimguiVersion = "v0.7"
	compile(group = "org.ice1000.jimgui", name = "kotlin-dsl", version = jimguiVersion)
	compile(group = "org.ice1000.jimgui", name = "extension", version = jimguiVersion)
	compile(group = "org.ice1000.textseq", name = "impl-gap", version = "v0.3")
}
