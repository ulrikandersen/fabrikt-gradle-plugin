package ch.acanda.gradle.fabrikt.generator

import ch.acanda.gradle.fabrikt.GenerateTaskConfiguration
import com.cjbooms.fabrikt.cli.ClientCodeGenOptionType
import com.cjbooms.fabrikt.cli.CodeGenerationType
import com.cjbooms.fabrikt.cli.ControllerCodeGenOptionType
import com.cjbooms.fabrikt.cli.ModelCodeGenOptionType
import org.gradle.api.provider.Provider

internal const val ARG_API_FILE = "--api-file"
internal const val ARG_API_FRAGMENT = "--api-fragment"
internal const val ARG_EXT_REF_RESOLUTION = "--external-ref-resolution"
internal const val ARG_BASE_PACKAGE = "--base-package"
internal const val ARG_OUT_DIR = "--output-directory"
internal const val ARG_SRC_PATH = "--src-path"
internal const val ARG_RESOURCES_PATH = "--resources-path"
internal const val ARG_TYPE_OVERRIDES = "--type-overrides"
internal const val ARG_VALIDATION_LIB = "--validation-library"
internal const val ARG_TARGETS = "--targets"
internal const val ARG_CLIENT_OPTS = "--http-client-opts"
internal const val ARG_CLIENT_TARGET = "--http-client-target"
internal const val ARG_CONTROLLER_OPTS = "--http-controller-opts"
internal const val ARG_CONTROLLER_TARGET = "--http-controller-target"
internal const val ARG_MODEL_OPTS = "--http-model-opts"

internal data class FabriktArguments(private val config: GenerateTaskConfiguration) {

    fun getCliArgs(): Array<String> = with(config) {
        @Suppress("ArgumentListWrapping")
        val args = mutableListOf(
            ARG_API_FILE, apiFile.asFile.get().absolutePath,
            ARG_BASE_PACKAGE, basePackage.get().toString(),
            ARG_OUT_DIR, outputDirectory.asFile.get().absolutePath,
        )
        apiFragments.forEach { fragment ->
            args.add(ARG_API_FRAGMENT)
            args.add(fragment.absolutePath)
        }
        externalReferenceResolution.orNull?.let { resolution ->
            args.add(ARG_EXT_REF_RESOLUTION)
            args.add(resolution.fabriktOption.name)
        }
        sourcesPath.orNull?.let { path ->
            args.add(ARG_SRC_PATH)
            args.add(path.toString())
        }
        resourcesPath.orNull?.let { path ->
            args.add(ARG_RESOURCES_PATH)
            args.add(path.toString())
        }
        validationLibrary.orNull?.let { library ->
            args.add(ARG_VALIDATION_LIB)
            args.add(library.fabriktOption.name)
        }
        if (quarkusReflectionConfig.get()) {
            args.add(ARG_TARGETS)
            args.add(CodeGenerationType.QUARKUS_REFLECTION_CONFIG.name)
        }
        addTypeOverridesArgs(args)
        addClientArgs(args)
        addControllerArgs(args)
        addModelArgs(args)
        return args.toTypedArray()
    }

    private fun GenerateTaskConfiguration.addTypeOverridesArgs(args: MutableList<String>) = with(typeOverrides) {
        val overrides = listOfNotNull(
            datetime.orNull?.fabriktOption
        )
        if (overrides.isNotEmpty()) {
            args.add(ARG_TYPE_OVERRIDES)
            args.addAll(overrides.map { it.name })
        }
    }

    private fun GenerateTaskConfiguration.addClientArgs(args: MutableList<String>) = with(client) {
        if (generate.get()) {
            args.add(ARG_TARGETS)
            args.add(CodeGenerationType.CLIENT.name)
            args.addIfEnabled(resilience4j, ARG_CLIENT_OPTS, ClientCodeGenOptionType.RESILIENCE4J)
            args.addIfEnabled(suspendModifier, ARG_CLIENT_OPTS, ClientCodeGenOptionType.SUSPEND_MODIFIER)
            target.orNull?.let {
                args.add(ARG_CLIENT_TARGET)
                args.add(it.fabriktOption.name)
            }
        }
    }

    private fun GenerateTaskConfiguration.addControllerArgs(args: MutableList<String>) = with(controller) {
        if (generate.get()) {
            args.add(ARG_TARGETS)
            args.add(CodeGenerationType.CONTROLLERS.name)
            args.addIfEnabled(authentication, ARG_CONTROLLER_OPTS, ControllerCodeGenOptionType.AUTHENTICATION)
            args.addIfEnabled(suspendModifier, ARG_CONTROLLER_OPTS, ControllerCodeGenOptionType.SUSPEND_MODIFIER)
            target.orNull?.let {
                args.add(ARG_CONTROLLER_TARGET)
                args.add(it.fabriktOption.name)
            }
        }
    }

    private fun GenerateTaskConfiguration.addModelArgs(args: MutableList<String>) = with(model) {
        if (generate.get()) {
            args.add(ARG_TARGETS)
            args.add(CodeGenerationType.HTTP_MODELS.name)
            args.addIfEnabled(extensibleEnums, ARG_MODEL_OPTS, ModelCodeGenOptionType.X_EXTENSIBLE_ENUMS)
            args.addIfEnabled(javaSerialization, ARG_MODEL_OPTS, ModelCodeGenOptionType.JAVA_SERIALIZATION)
            args.addIfEnabled(quarkusReflection, ARG_MODEL_OPTS, ModelCodeGenOptionType.QUARKUS_REFLECTION)
            args.addIfEnabled(micronautIntrospection, ARG_MODEL_OPTS, ModelCodeGenOptionType.MICRONAUT_INTROSPECTION)
            args.addIfEnabled(micronautReflection, ARG_MODEL_OPTS, ModelCodeGenOptionType.MICRONAUT_REFLECTION)
            args.addIfEnabled(includeCompanionObject, ARG_MODEL_OPTS, ModelCodeGenOptionType.INCLUDE_COMPANION_OBJECT)
            args.addIfEnabled(
                sealedInterfacesForOneOf,
                ARG_MODEL_OPTS,
                ModelCodeGenOptionType.SEALED_INTERFACES_FOR_ONE_OF
            )
        }
    }

    private fun MutableList<String>.addIfEnabled(provider: Provider<Boolean>, argName: String, argValue: Enum<*>) {
        if (provider.getOrElse(false)) {
            this.add(argName)
            this.add(argValue.name)
        }
    }

}
