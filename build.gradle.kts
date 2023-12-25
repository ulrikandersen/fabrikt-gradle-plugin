import ch.acanda.gradle.fabrikt.build.ExtensionGenerator
import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    kotlin("jvm")
    `java-gradle-plugin`
    idea
    id("io.gitlab.arturbosch.detekt") version "1.23.4"
}

group = "ch.acanda.gradle.fabrikt"
version = "0.1-SNAPSHOT"

val generatedSources: Provider<Directory> = project.layout.buildDirectory.dir("generated/src/main/kotlin")

gradlePlugin {
    plugins {
        create("fabriktPlugin") {
            id = project.name
            implementationClass = "$group.FabriktPlugin"
            displayName = "Fabrikt Gradle Plugin"
            description = "Generates Kotlin code from an OpenAPI 3 specification."
            tags.set(listOf("openapi", "openapi-3.0", "codegen", "kotlin", "fabrikt"))
        }
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.cjbooms:fabrikt:11.2.0")
    testImplementation(kotlin("test"))
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.4")
}

kotlin {
    jvmToolchain(17)
}

sourceSets {
    main {
        kotlin {
            srcDir(generatedSources)
        }
    }
}

idea {
    module {
        generatedSourceDirs.add(generatedSources.get().asFile)
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom("$projectDir/config/detekt.yaml")
}

testing {
    suites {
        @Suppress("unused")
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter("5.10.0")
            dependencies {
                val kotestVersion = "5.8.0"
                implementation("io.kotest:kotest-runner-junit5:$kotestVersion")
                implementation("io.kotest:kotest-property:$kotestVersion")
                runtimeOnly("org.junit.platform:junit-platform-launcher:1.10.1")
            }
        }
    }
}

tasks {

    val generateExtensions by creating(ExtensionGenerator::class.java) {
        outputDirectory.set(generatedSources)
    }

    compileKotlin {
        dependsOn(generateExtensions)
    }

    wrapper {
        gradleVersion = "8.5"
    }

    withType<Detekt>().configureEach {
        val compilation =
            project.extensions.getByType<KotlinJvmProjectExtension>().target.compilations.getByName("test")
        classpath.setFrom(compilation.output.classesDirs, compilation.compileDependencyFiles)

        reports {
            xml.required.set(false)
            html.required.set(false)
            txt.required.set(false)
            sarif.required.set(false)
            md.required.set(true)
        }
    }

}
