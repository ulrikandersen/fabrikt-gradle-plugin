package ch.acanda.gradle.fabrikt

import io.kotest.core.spec.style.StringSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.paths.exist
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.File

class GradleTest : StringSpec({

    "`gradle fabriktGenerate` with full configuration should run fabrikt" {
        val projectDir = tempdir("project")
        val basePackage = "ch.acanda"
        val outputPath = "build/generated/custom"
        val openapiPath = createSpec(projectDir)
        projectDir.resolve("build.gradle.kts").writeText(
            """
            |plugins {
            |  id("ch.acanda.gradle.fabrikt")
            |}
            |
            |fabrikt {
            |  generate("dog") { 
            |      apiFile.set(file("$openapiPath"))
            |      basePackage.set("$basePackage")
            |      outputDirectory.set(file("$outputPath"))
            |  }
            |}
            """.trimMargin()
        )

        val result = runFabriktGenerate(projectDir)

        result.task(":fabriktGenerate")
            .shouldNotBeNull()
            .outcome shouldBe TaskOutcome.SUCCESS

        val modelsPath = "$outputPath/src/main/kotlin/${basePackage.replace('.', '/')}/models"
        projectDir.resolve("$modelsPath/Dog.kt").toPath() should exist()
    }

    "`gradle fabriktGenerate` with minimal configuration should run fabrikt" {
        val projectDir = tempdir("project")
        val basePackage = "ch.acanda"
        val openapiPath = createSpec(projectDir)
        projectDir.resolve("build.gradle.kts").writeText(
            """
            |plugins {
            |    id("ch.acanda.gradle.fabrikt")
            |}
            |
            |fabrikt {
            |  generate("dog") {
            |    apiFile.set(file("$openapiPath"))
            |    basePackage.set("$basePackage")
            |  }
            |}
            """.trimMargin()
        )
        val result = runFabriktGenerate(projectDir)

        result.task(":fabriktGenerate")
            .shouldNotBeNull()
            .outcome shouldBe TaskOutcome.SUCCESS

        val modelsPath = "build/generated/fabrikt/src/main/kotlin/${basePackage.replace('.', '/')}/models"
        projectDir.resolve("$modelsPath/Dog.kt").toPath() should exist()
    }

    "`gradle fabriktGenerate` with multiple kotlin configurations should run fabrikt" {
        val projectDir = tempdir("project")
        val basePackage = "ch.acanda"
        val openapiPathDog = createSpec(projectDir, "Dog")
        val openapiPathCat = createSpec(projectDir, "Cat")
        projectDir.resolve("build.gradle.kts").writeText(
            """
            |plugins {
            |    id("ch.acanda.gradle.fabrikt")
            |}
            |
            |fabrikt {
            |  generate("dog") {
            |    apiFile.set(file("$openapiPathDog"))
            |    basePackage.set("$basePackage")
            |  }
            |  generate("cat") {
            |    apiFile.set(file("$openapiPathCat"))
            |    basePackage.set("$basePackage")
            |  }
            |}
            """.trimMargin()
        )
        val result = runFabriktGenerate(projectDir)

        result.task(":fabriktGenerate")
            .shouldNotBeNull()
            .outcome shouldBe TaskOutcome.SUCCESS

        val modelsPath = "build/generated/fabrikt/src/main/kotlin/${basePackage.replace('.', '/')}/models"
        val files = projectDir.resolve(modelsPath).walkTopDown().toList()
        files shouldContain projectDir.resolve("$modelsPath/Dog.kt")
        files shouldContain projectDir.resolve("$modelsPath/Cat.kt")
        projectDir.resolve("$modelsPath/Dog.kt").toPath() should exist()
        projectDir.resolve("$modelsPath/Cat.kt").toPath() should exist()
    }

    "`gradle fabriktGenerate` with multiple groovy configurations should run fabrikt" {
        val projectDir = tempdir("project")
        val basePackage = "ch.acanda"
        val openapiPathDog = createSpec(projectDir, "Dog")
        val openapiPathCat = createSpec(projectDir, "Cat")
        projectDir.resolve("build.gradle").writeText(
            """
            |plugins {
            |    id 'ch.acanda.gradle.fabrikt'
            |}
            |
            |fabrikt {
            |  dog {
            |    apiFile = file('$openapiPathDog')
            |    basePackage = '$basePackage'
            |  }
            |  cat {
            |    apiFile = file('$openapiPathCat')
            |    basePackage = '$basePackage'
            |  }
            |}
            """.trimMargin()
        )
        val result = runFabriktGenerate(projectDir)

        result.task(":fabriktGenerate")
            .shouldNotBeNull()
            .outcome shouldBe TaskOutcome.SUCCESS

        val modelsPath = "build/generated/fabrikt/src/main/kotlin/${basePackage.replace('.', '/')}/models"
        val files = projectDir.resolve(modelsPath).walkTopDown().toList()
        files shouldContain projectDir.resolve("$modelsPath/Dog.kt")
        files shouldContain projectDir.resolve("$modelsPath/Cat.kt")
        projectDir.resolve("$modelsPath/Dog.kt").toPath() should exist()
        projectDir.resolve("$modelsPath/Cat.kt").toPath() should exist()
    }

}) {

    companion object {

        private fun createSpec(projectDir: File, name: String = "Dog"): String {
            val specPath = "src/main/openapi/$name.yaml"
            val specFile = projectDir.resolve(specPath)
            specFile.parentFile.mkdirs()
            specFile.writeText(
                """
                |openapi: 3.0.3
                |info:
                |  title: $name
                |  version: 1.0.0
                |paths: {}
                |components:
                |  schemas: 
                |    $name:
                |      type: object
                |      properties:
                |        isGoodBoy:
                |          type: boolean
                |          default: true
                """.trimMargin()
            )
            return specPath
        }

        private fun runFabriktGenerate(projectDir: File): BuildResult =
            GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments("fabriktGenerate")
                .withPluginClasspath()
                .build()

    }

}