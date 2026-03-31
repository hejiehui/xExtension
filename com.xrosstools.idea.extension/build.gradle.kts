plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.10.2"
}

group = "com.xrosstools"
version = "2026.1.1"

val sandbox  : String by project

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaUltimate("2025.3.3")
    }

    compileOnly(files("$sandbox/com.xrosstools.idea.gef.zip"))
}

intellijPlatform {
    instrumentCode = true

    pluginConfiguration {
        name = "Xross Tools Extension"

        productDescriptor {
            code = "PXTENSION"
            releaseDate = "20260204"
            releaseVersion = "20261"
        }

        ideaVersion {
            sinceBuild = "183.6156.11"
        }

        changeNotes = """
            <em>2026.1.1</em> Optimize config loading and notify what config item is missing.<br>
            <em>2026.1.0</em> Revise prompts and support stream mode when generate/update model.<br>
            <em>2025.3.0</em> Support all x-series models, including xross behavior, unit, state and decision.<br>
            <em>1.0</em> Inital version, provide search and export ability.<br>
        """.trimIndent()
    }

    pluginVerification {
        ides {
            // 验证最老和最新的目标版本
            ide("IC-2018.3.6")
            ide("IC-2020.3.4")
            ide("IC-2025.3.3")
        }
    }
}

intellijPlatformTesting {
    runIde {
        register("runWithLocalPlugins") {
            plugins {
                val pluginFiles = file(sandbox).listFiles()
                pluginFiles.forEach { file ->
                    if (!file.name.contains(project.name)) {
                        localPlugin(file.absolutePath)
                    }
                }
            }
        }
    }
}

tasks.named("runIde") {
    dependsOn("runWithLocalPlugins")
}

tasks {
    buildPlugin {
        archiveFileName.set("${project.name}.zip")
    }
}
