package eu.niton.ktx.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSFile
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import eu.niton.ktx.Content
import eu.niton.ktx.ContentDefinition
import eu.niton.ktx.StringContent
import eu.niton.ktx.KtxElement
import java.net.URL

data class Generator(
    val pkg: String,
    val filer: CodeGenerator,
    val logger: KSPLogger,
    val rootFile: KSFile,
    val xsd: URL
) {
    val htmlSchema = lazy { parseXmlSchema(xsd) }
    
    fun writeFile(filename: String, packageName: String, fileContent: Collection<FileContent>) {
        val file = filer.createNewFile(Dependencies(false, rootFile), packageName, filename)
        file.bufferedWriter(Charsets.UTF_8).use {
            val fileSpec = FileSpec.builder(packageName, filename)
            
            // Add necessary imports for generated functions
            if (filename == "TagExtensions") {
                // No need to import tag since we're in the same package
            }
            
            fileContent.forEach {
                when (it) {
                    is FileContent.Function -> fileSpec.addFunction(it.function)
                    is FileContent.Type -> fileSpec.addType(it.type)
                    is FileContent.TypeAlias -> fileSpec.addTypeAlias(it.spec)
                }
            }
            try {
                fileSpec.build().writeTo(it)
                it.flush()
            } catch (e: Exception) {
                logger.error("Failed to write ${fileSpec.packageName}.${fileSpec.name}", rootFile)
                throw e
            }
        }
    }

    fun TypeSpec.asFile(subpackage: String, filename: String? = null): FileContent.Type {
        return FileContent.Type(this, filename ?: requireNotNull(this.name), "$pkg.$subpackage")
    }

    fun FunSpec.asFile(subpackage: String, filename: String): FileContent.Function {
        return FileContent.Function(this, filename, "$pkg.$subpackage")
    }

    fun TypeAliasSpec.asFile(subpackage: String, filename: String): FileContent.TypeAlias {
        return FileContent.TypeAlias(this, filename, "$pkg.$subpackage")
    }
}

// Data structures for new generation pattern
data class TagInterface(
    val tagName: String,
    val interfaceSpec: FileContent.Type,
    val extensionFunctionSpec: FileContent.Function
)

private val tags = mutableMapOf<String, TagInterface>()
private val tagGroups = mutableMapOf<String, FileContent.Type>()

fun Generator.generate() {
    // Generate per-tag interfaces and extension functions
    htmlSchema.value.tags.forEach { (_, tagInfo) ->
        generateTagInterface(tagInfo)
    }
    
    // TODO: Generate content definition interfaces, classes, and render functions
    
    // Collect all generated content and write to files
    val allContent = mutableListOf<FileContent>()
    allContent.addAll(tags.values.flatMap { listOf(it.interfaceSpec, it.extensionFunctionSpec) })
    allContent.addAll(tagGroups.values)
    
    // Group by package and file
    val files = allContent
        .groupBy { it.packageName }
        .mapValues { it.value.groupBy { it.fileName } }

    files.forEach { (packageName, files) ->
        files.forEach { (fileName, contents) ->
            writeFile(fileName, packageName, contents)
        }
    }
}

private val contentDefinitionClass = ContentDefinition::class.asClassName()
private val contentClass = Content::class.asClassName()
private val strContentClass = StringContent::class.asClassName()
private val ktxElementClass = KtxElement::class.asClassName()

// Generate per-tag interface and extension function following the target pattern
fun Generator.generateTagInterface(tag: TagInfo): TagInterface {
    val tagName = tag.name
    val className = "${tagName.humanize().capitalize()}HtmlTag"
    
    // Create the per-tag interface (e.g., DivHtmlTag<T> : ContentDefinition<T>)
    val tagGeneric = TypeVariableName("T")
    val interfaceClassName = "eu.niton.ktx.tags.$className"
    val interfaceSpec = TypeSpec.interfaceBuilder(className)
        .addTypeVariable(
            tagGeneric.copy(
                bounds = listOf(
                    ClassName.bestGuess(interfaceClassName).parameterizedBy(tagGeneric),
                    contentClass.parameterizedBy(tagGeneric)
                )
            )
        )
        .addSuperinterface(contentDefinitionClass.parameterizedBy(tagGeneric))
        .build()
        .asFile("tags")
    
    // Create the extension function (e.g., inline fun DivHtmlTag<*>.div(...) = tag(...))
    val funSpec = generateTagExtensionFunction(tag, interfaceClassName)
    
    val tagInterface = TagInterface(tagName, interfaceSpec, funSpec)
    tags[tagName] = tagInterface
    return tagInterface
}

// Generate the extension function for a tag
private fun Generator.generateTagExtensionFunction(tag: TagInfo, interfaceClassNameFully: String): FileContent.Function {
    val tagName = tag.name
    val funBuilder = FunSpec.builder(tagName)
        .addModifiers(KModifier.INLINE)
        .receiver(ClassName.bestGuess(interfaceClassNameFully).parameterizedBy(STAR))
    
    // Add attribute parameters
    val attributeParams = mutableListOf<ParameterSpec>()
    val eventParams = mutableListOf<ParameterSpec>()
    
    tag.content.allAttributes.sortedByDescending { it.required }.forEach { attribute ->
        when (attribute.type) {
            is AttributeType.EventHandler -> {
                val paramType = LambdaTypeName.get(
                    parameters = listOf(ParameterSpec.unnamed(STRING.copy(nullable = true))),
                    returnType = UNIT
                ).copy(nullable = true)
                val param = ParameterSpec.builder(attribute.name, paramType)
                    .addModifiers(KModifier.NOINLINE) // Add noinline for nullable lambda parameters
                    .defaultValue("null")
                    .build()
                eventParams.add(param)
                funBuilder.addParameter(param)
            }
            else -> {
                val paramType = LambdaTypeName.get(
                    returnType = STRING.copy(nullable = true)
                ).copy(nullable = true)
                val param = ParameterSpec.builder(attribute.name.replaceIfReserved(), paramType)
                    .addModifiers(KModifier.NOINLINE) // Add noinline for nullable lambda parameters
                    .defaultValue("null")
                    .build()
                attributeParams.add(param)
                funBuilder.addParameter(param)
            }
        }
    }
    
    // Add body parameter based on tag content
    val contentType = getContentType(tag.content, "${tagName}Content")
    val bodyParam = if (contentType != null) {
        val bodyType = LambdaTypeName.get(
            receiver = contentType,
            returnType = UNIT
        )
        val param = ParameterSpec.builder("body", bodyType).build()
        funBuilder.addParameter(param)
        param
    } else null
    
    // Generate the function body
    val attributeMap = if (attributeParams.isEmpty()) "mapOf()" else {
        "mapOf(" + attributeParams.joinToString(", ") { param ->
            "\"${param.name}\" to ${param.name}"
        } + ")"
    }
    
    val eventMap = if (eventParams.isEmpty()) "mapOf()" else {
        "mapOf(" + eventParams.joinToString(", ") { param ->
            "\"${param.name}\" to ${param.name}"
        } + ")"
    }
    
    val bodyExpression = if (bodyParam != null && contentType != null) {
        "render(${bodyParam.name})"
    } else "null"
    
    funBuilder.addStatement(
        "return tag(%S, %L, %L, %L)",
        tagName,
        attributeMap,
        eventMap,
        bodyExpression
    )
    
    return funBuilder.build().asFile("", "TagExtensions")
}

// Simplified version of getContentType for now
private fun Generator.getContentType(content: TagContent, fallbackName: String): ClassName? {
    // For now, just return null for simplicity - we'll implement this later
    return null
}

// Helper function to get the class name for a tag
private fun TagInfo.className(): String = "${name.humanize().capitalize()}HtmlTag"