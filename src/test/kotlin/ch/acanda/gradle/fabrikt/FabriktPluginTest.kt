package ch.acanda.gradle.fabrikt

import ch.acanda.gradle.fabrikt.matchers.shouldBeEmpty
import ch.acanda.gradle.fabrikt.matchers.shouldContain
import ch.acanda.gradle.fabrikt.matchers.shouldContainExactly
import ch.acanda.gradle.fabrikt.matchers.shouldContainString
import com.cjbooms.fabrikt.cli.ClientCodeGenOptionType
import com.cjbooms.fabrikt.cli.ClientCodeGenTargetType
import com.cjbooms.fabrikt.cli.ControllerCodeGenOptionType
import com.cjbooms.fabrikt.cli.ControllerCodeGenTargetType
import com.cjbooms.fabrikt.cli.ModelCodeGenOptionType
import io.kotest.core.spec.style.WordSpec
import io.kotest.engine.spec.tempdir
import io.kotest.engine.spec.tempfile
import io.kotest.matchers.collections.beEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.types.shouldBeInstanceOf
import org.gradle.testfixtures.ProjectBuilder

/**
 * Tests that the FabriktPlugin:
 * - creates its extensions
 * - registers the task "fabriktGenerate"
 *     - sets all its properties correctly
 *     - uses the proper defaults where the properties are not set
 */
class FabriktPluginTest : WordSpec({

    "The fabrikt plugin" should {
        "register the task fabriktGenerate with a full configuration" {
            val project = ProjectBuilder.builder().build()
            val apiFile = tempfile("apiSpec", ".yaml")
            val apiFragment = tempfile("apiFragment", ".yaml")
            val basePackage = "ch.acanda"
            val outDir = tempdir("out")
            val srcDir = "src/fabrikt/kotlin"
            val resDir = "src/fabrikt/res"

            project.pluginManager.apply("ch.acanda.gradle.fabrikt")
            project.extensions.configure(FabriktExtension::class.java) { ext ->
                ext.generate("api") {
                    it.apiFile(apiFile)
                    it.apiFragments(apiFragment)
                    it.basePackage(basePackage)
                    it.outputDirectory(outDir)
                    it.sourcesPath(srcDir)
                    it.resourcesPath(resDir)
                    with(it.client) {
                        enabled(true)
                        options(RESILIENCE4J)
                        target(OPEN_FEIGN)
                    }
                    with(it.controller) {
                        enabled(true)
                        options(AUTHENTICATION)
                        target(MICRONAUT)
                    }
                    with(it.model) {
                        enabled(false)
                        options(JAVA_SERIALIZATION)
                    }
                }
            }

            project.tasks.findByName("fabriktGenerate")
                .shouldNotBeNull()
                .shouldBeInstanceOf<FabriktGenerateTask>()
                .configurations.get().shouldHaveSize(1)
                .first().run {
                    this.apiFile shouldContain apiFile
                    this.apiFragments.files shouldContainExactly listOf(apiFragment)
                    this.basePackage shouldContainString basePackage
                    this.outputDirectory shouldContain outDir
                    this.sourcesPath shouldContainString srcDir
                    this.resourcesPath shouldContainString resDir
                    with(client) {
                        enabled shouldContain true
                        options shouldContainExactly ClientCodeGenOptionType.RESILIENCE4J
                        target shouldContain ClientCodeGenTargetType.OPEN_FEIGN
                    }
                    with(controller) {
                        enabled shouldContain true
                        options shouldContainExactly ControllerCodeGenOptionType.AUTHENTICATION
                        target shouldContain ControllerCodeGenTargetType.MICRONAUT
                    }
                    with(model) {
                        enabled shouldContain false
                        options shouldContainExactly ModelCodeGenOptionType.JAVA_SERIALIZATION
                    }
                }
        }

        "register the task fabriktGenerate with a minimal configuration" {
            val project = ProjectBuilder.builder().build()
            val apiFile = tempfile("apiSpec", ".yaml")
            val basePackage = "ch.acanda"
            val outDir = tempdir("out")

            project.pluginManager.apply("ch.acanda.gradle.fabrikt")
            project.extensions.configure(FabriktExtension::class.java) { ext ->
                ext.generate("api") {
                    it.apiFile(apiFile)
                    it.basePackage(basePackage)
                    it.outputDirectory(outDir)
                }
            }

            project.tasks.findByName("fabriktGenerate")
                .shouldNotBeNull()
                .shouldBeInstanceOf<FabriktGenerateTask>()
                .configurations.get().shouldHaveSize(1)
                .first().run {
                    this.apiFile shouldContain apiFile
                    this.apiFragments.files should beEmpty()
                    this.basePackage shouldContainString basePackage
                    this.outputDirectory shouldContain outDir
                    this.sourcesPath shouldContain "src/main/kotlin"
                    this.resourcesPath shouldContain "src/main/resources"
                    with(client) {
                        enabled shouldContain false
                        options.shouldBeEmpty()
                        target shouldContain ClientCodeGenTargetType.OK_HTTP
                    }
                    with(controller) {
                        enabled shouldContain false
                        options.shouldBeEmpty()
                        target shouldContain ControllerCodeGenTargetType.SPRING
                    }
                    with(model) {
                        enabled shouldContain true
                        options.shouldBeEmpty()
                    }
                }
        }
    }

})
