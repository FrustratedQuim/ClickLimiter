plugins {
    kotlin("jvm") version "2.2.10"
    id("com.gradleup.shadow") version "9.0.2"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.18"
}

group = "org.clicklimiter"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://jitpack.io")

    // PacketEvents
    maven { url = uri("https://repo.codemc.io/repository/maven-releases/") }
    maven { url = uri("https://repo.codemc.io/repository/maven-snapshots/") }
}

dependencies {
    paperweight.paperDevBundle("1.21.1-R0.1-SNAPSHOT")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.2.10")
    implementation("net.kyori:adventure-text-minimessage:4.24.0")
    compileOnly("com.github.retrooper:packetevents-spigot:2.9.4")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

val plainJarName = "${project.name}-${project.version}_1.21.1.jar"
val shadowJarName = "${project.name}-${project.version}_1.21.1-shadow.jar"

tasks {
    compileKotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    compileJava {
        options.release.set(21)
    }

    processResources {
        filteringCharset = Charsets.UTF_8.name()
        val props = mapOf(
            "name" to project.name,
            "version" to project.version
        )
        inputs.properties(props)
        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }

    shadowJar {
        archiveFileName.set(shadowJarName)
        minimize()
        relocate("kotlin", "${project.group}.kotlin")
        relocate("io.papermc.lib", "${project.group}.paperlib")
        relocate("org.jetbrains.annotations", "${project.group}.jetbrains.annotations")
        relocate("org.intellij.lang.annotations", "${project.group}.intellij.lang.annotations")
    }

    jar {
        archiveFileName.set(plainJarName)
    }

    build {
        dependsOn(shadowJar)
    }

    val copyPluginJar by registering(Copy::class) {
        from("$buildDir/libs/$shadowJarName")
        into("C:/Users/Home/Desktop/Архив/Minecraft/Сервера/Creative_1.21.1/plugins")
        doLast {
            println("Plugin moved to server plugins.")
        }
    }

    build {
        finalizedBy(copyPluginJar)
    }
}
