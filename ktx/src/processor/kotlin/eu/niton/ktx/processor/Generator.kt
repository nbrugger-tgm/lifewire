package eu.niton.ktx.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSFile
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec.Companion.anonymousClassBuilder
import eu.niton.ktx.Content
import eu.niton.ktx.ContentDefinition
import eu.niton.ktx.StringContent
import java.net.URI
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
            
            // Add imports for runtime functions if this file contains functions
            if (fileContent.any { it is FileContent.Function }) {
                fileSpec.addImport("eu.niton.ktx", "tag")
                fileSpec.addImport("eu.niton.ktx", "render")
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

data class TagFile(
    val tagInterface: FileContent.Type,
    val tagFunction: FileContent.Function,
    val bodyTypeAlias: FileContent.TypeAlias?
)

private val tags = mutableMapOf<String, TagFile>()
private val attributeGroups = mutableMapOf<String, FileContent.Type>()
private val tagGroups = mutableMapOf<String, FileContent.Type>()

fun Generator.generate() {
    htmlSchema.value.tags.forEach {
        getTag(it.value)
    }
    val tagFilecontent = tags.values.flatMap { listOfNotNull(it.tagInterface, it.tagFunction, it.bodyTypeAlias) }
    val files = (tagFilecontent + tagGroups.values + attributeTypes.values)
        .groupBy { it.packageName }
        .mapValues { it.value.groupBy { it.fileName } }

    files.forEach { (packageName, files) ->
        files.forEach { (fileName, contents) ->
            writeFile(fileName, packageName, contents)
        }
    }
}

private val contentClass = Content::class.asClassName()
private val strContentClass = StringContent::class.asClassName()

fun Generator.getTag(tag: TagInfo): TagFile {
    return tags.nonConcurrentComputeIfAbsent(tag.name) {
        // Create the per-tag interface (e.g., DivHtmlTag<T> : ContentDefinition<T>)
        val className = "${tag.name.humanize().capitalize()}HtmlTag"
        val interfaceClassName = "eu.niton.ktx.tags.$className"
        
        val contentDefinitionClass = ContentDefinition::class.asClassName()
        val contentClass = Content::class.asClassName()
        
        val tagInterface = TypeSpec.interfaceBuilder(className)
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
            
        // Create the extension function that calls tag()
        val tagFunction = generateTagExtensionFunction(tag, interfaceClassName)
        
        // Generate content type and body type alias using existing logic
        // val contentType = getContentType(tag.content, tag.name + "content", filename = className)
        // TODO: Temporarily disable body type alias until content generation is fixed
        val bodyTypeAlias = null/*contentType?.let {
            val contentFnType = LambdaTypeName.get(receiver = it.parameterizedBy(tagGeneric), returnType = UNIT)
            TypeAliasSpec.builder(tag.name.humanize().capitalize() + "Body", contentFnType)
                .addTypeVariable(tagGeneric)
                .build().asFile("tags", className)
        }*/
        
        TagFile(tagInterface, tagFunction, bodyTypeAlias)
    }
}

// Generate the extension function for a tag that calls the new runtime API
private fun Generator.generateTagExtensionFunction(tag: TagInfo, interfaceClassNameFully: String): FileContent.Function {
    val tagName = tag.name
    val className = tag.className()
    val funBuilder = FunSpec.builder(tagName)
        .addModifiers(KModifier.INLINE)
        .receiver(ClassName.bestGuess(interfaceClassNameFully).parameterizedBy(STAR))
    
    // Use existing logic to add attribute parameters
    tag.content.allAttributes.sortedByDescending { it.required }.forEach { attribute ->
        addAttributeParameter(attribute, tag, funBuilder)
    }
    
    // Add body parameter using existing content type logic  
    // val contentType = getContentType(tag.content, tag.name + "content", filename = className)
    // TODO: Temporarily disable body parameters until content generation is fixed
    val contentType: ClassName? = null
    val bodyParam = null/*contentType?.let {
        val parameterizedContent = it.parameterizedBy(tagGeneric)
        val contentFnType = LambdaTypeName.get(receiver = parameterizedContent, returnType = UNIT)
        val param = ParameterSpec.builder("body", contentFnType).build()
        funBuilder.addParameter(param)
        param
    }*/
    
    // Generate function body that calls tag() with new runtime API
    val attributeMapCode = getAttributeMap(tag)
    val eventMapCode = getEventListenerMap(tag)
    val bodyExpression = "null" // TODO: Re-enable when content generation is fixed
    /*if (contentType != null && bodyParam != null) {
        "render(${bodyParam.name})"
    } else "null"*/
    
    funBuilder.addStatement(
        "return tag(%S, %L, %L, %L)",
        tagName,
        attributeMapCode,
        eventMapCode,
        bodyExpression
    )
    
    return funBuilder.build().asFile("tags", className)
}



private val tagGeneric = TypeVariableName("T")


private fun Generator.getEventListenerMap(tag: TagInfo): CodeBlock {
    val code = CodeBlock.builder()
    code.add("mapOf(\n")
    tag.content.allAttributes.filter { it.type is AttributeType.EventHandler }.forEach { attribute ->
        code.add("%S to %N,\n", attribute.name, attribute.name.humanize())
    }
    code.add(")")
    return code.build()
}

private fun Generator.getAttributeMap(tag: TagInfo): CodeBlock {
    val code = CodeBlock.builder()
    code.add("mapOf(\n")
    tag.content.allAttributes.filter { it.type !is AttributeType.EventHandler }.forEach { attribute ->
        code.add("%S to %L,\n", attribute.name, getAttributeValueStringFn(attribute.type, attribute.name.humanize()))
    }
    code.add(")")
    return code.build()
}

private fun Generator.getAttributeValueStringFn(
    type: AttributeType,
    variable: String
): CodeBlock {
    if (type == AttributeType.String) return CodeBlock.of("%N", variable)
    val conversionCode = when (type) {
        is AttributeType.Enum -> CodeBlock.of("%N()?.%N", "it", "value")
        AttributeType.EventHandler -> throw UnsupportedOperationException("event handlers are not considered attributes")
        is AttributeType.NamedBoolean -> CodeBlock.of(
            "%N()?.let { if(it) %S else %S }",
            "it",
            type.trueName,
            type.falseName
        )

        AttributeType.Number, AttributeType.String,
        AttributeType.URI, AttributeType.Boolean -> CodeBlock.of("%N()?.toString()", "it")

        AttributeType.Toggle -> CodeBlock.of("if(%N()?:false) \"true\" else null", "it")
    }
    return CodeBlock.builder()
        .add("%N?.let {", variable)
        .add("({ %L })", conversionCode)
        .add("}")
        .build()
}

private fun Generator.addAttributeParameter(
    attribute: AttributeInfo,
    tag: TagInfo,
    tagFn: FunSpec.Builder
) {
    val qualifiedName = attribute.fullyQualifiedName(tag)
    val attributeType = getAttributeType(
        attribute.name,
        qualifiedName,
        attribute.type
    ).copy(nullable = !attribute.required)
    val type = attributeType as? LambdaTypeName ?: LambdaTypeName.get(returnType = attributeType)
        .copy(nullable = !attribute.required)
    val param = ParameterSpec.builder(attribute.name.humanize(), type)
        .addModifiers(KModifier.NOINLINE) // Add noinline for nullable lambda parameters
    if (!attribute.required) {
        val defaultValue = if (attribute.defaultValue != null) {
            CodeBlock.builder().beginControlFlow("").add(
                getAttributeLiteral(attribute.name, qualifiedName, attribute.type, attribute.defaultValue)
            ).endControlFlow().build()
        } else CodeBlock.of("null")
        param.defaultValue(defaultValue)
    }
    tagFn.addParameter(param.build())
}

private fun AttributeInfo.fullyQualifiedName(tag: TagInfo) = if (global) "${tag.name}-${name}" else name

private fun Generator.getAttributeLiteral(
    attributeName: String,
    fullyQualifiedAttributeName: String,
    type: AttributeType,
    value: String
): CodeBlock {
    return when (type) {
        AttributeType.Boolean -> CodeBlock.of(value)
        is AttributeType.Enum -> {
            val enum = getAttributeEnum(attributeName, fullyQualifiedAttributeName, type)
            CodeBlock.of("%T.%N", enum.typeName(), value.enumEntryName())
        }

        AttributeType.EventHandler -> throw IllegalArgumentException("Event handler cannot have default value ($value)")
        is AttributeType.NamedBoolean -> CodeBlock.of("%S", value)
        AttributeType.Number -> CodeBlock.of("%L", value)
        AttributeType.String -> CodeBlock.of("%S", value)
        AttributeType.Toggle -> throw IllegalArgumentException("How to handle toggle default value ($value)???")
        AttributeType.URI -> CodeBlock.of("%T(%S)", URI::class.asClassName(), value)
    }
}

private fun Generator.getAttributeType(
    attributeName: String,
    fullyQualifiedAttributeName: String,
    type: AttributeType
): TypeName {
    return when (type) {
        AttributeType.Boolean -> BOOLEAN
        is AttributeType.NamedBoolean -> BOOLEAN
        AttributeType.Toggle -> BOOLEAN
        AttributeType.EventHandler -> LambdaTypeName.get(
            parameters = listOf(ParameterSpec.unnamed(STRING.copy(nullable = true))),
            returnType = UNIT
        )
        AttributeType.Number -> FLOAT
        AttributeType.String -> STRING
        AttributeType.URI -> URI::class.asClassName()
        is AttributeType.Enum -> getAttributeEnum(attributeName, fullyQualifiedAttributeName, type).typeName()
    }
}

fun String.enumEntryName(): String =
    uppercase().replace("-", "_").replace("/", "_").replace("+", "_").replace(".", "_")

private val attributeTypes = mutableMapOf<String, FileContent.Type>()
private fun Generator.getAttributeEnum(
    attributeName: String,
    fullyQualifiedAttributeName: String,
    type: AttributeType.Enum
): FileContent.Type {
    val name = if (!type.global || type.name == null) fullyQualifiedAttributeName else type.name
    return attributeTypes.computeIfAbsent(name) {
        val subType = getAttributeType(attributeName, fullyQualifiedAttributeName, type.type)
        val spec = TypeSpec.enumBuilder(name.humanize().capitalize())
            .primaryConstructor(FunSpec.constructorBuilder().addParameter("value", subType).build())
            .addProperty(
                PropertySpec.builder("value", subType)
                    .initializer("value")
                    .build()
            )

        type.values.forEach { value ->
            if (value.isEmpty()) {
                if (!type.values.contains(attributeName)) spec.addEnumConstant(
                    attributeName.enumEntryName(),
                    anonymousClassBuilder().addSuperclassConstructorParameter("%S", value).build()
                )
                return@forEach
            }
            spec.addEnumConstant(
                value.enumEntryName(),
                anonymousClassBuilder().addSuperclassConstructorParameter("%S", value).build()
            )
        }

        spec.build().asFile("attributes")
    }
}

private fun TagInfo.className(): String = "${name.humanize().capitalize()}HtmlTag"


/**
 * Generates the interface of the "Body-receiver" the type that is given to the body-lambda
 */
private fun Generator.getContentType(content: TagContent, fallbackName: String, filename: String? = null): ClassName? {
    val requiredSuperinterfaces = content.childrenGroups.size +
            (if (content.allowText) 1 else 0) +
            (if (content.superType != null) 1 else 0)
    return if (content.directChildren.isNotEmpty() || requiredSuperinterfaces > 1) {
        val tagGroup = TagGroup(
            content.name ?: fallbackName,
            content.directChildren.toList(),
            content.childrenGroups
        );
        val superType = listOfNotNull(content.superType?.let { getContentType(it, fallbackName) })
        val packageName = if (content.name != null) "tags.content" else "tags"
        val fileName = if (content.name != null) null else filename
        generateTagGroup(
            tagGroup,
            packageName,
            fileName = fileName,
            allowText = content.allowText,
            superTypes = superType
        ).typeName()
    } else if (content.allowText) {
        strContentClass
    } else if (content.superType != null) {
        getContentType(content.superType!!, fallbackName)
    } else if (content.childrenGroups.size == 1) {
        generateTagGroup(content.childrenGroups.first()).typeName()
    } else null
}


fun Generator.generateTagGroup(
    group: TagGroup,
    packageName: String? = null,
    fileName: String? = null,
    allowText: Boolean = false,
    superTypes: Collection<ClassName> = emptyList()
): FileContent.Type {
    tagGroups[group.name]?.let {
        return it
    }

    val className = group.name.humanize().capitalize()
        .replace("Tag", "")
        .replace("Content", "")
        .replace("Group", "") + "Content"
    val contentInterface = TypeSpec.interfaceBuilder(className)
        .addTypeVariable(tagGeneric)
        .addSuperinterface(if (allowText) strContentClass.parameterizedBy(tagGeneric) else contentClass)
    val implementation = TypeSpec.classBuilder("Impl")
        .addTypeVariable(tagGeneric)
    if (allowText) {
        implementation.addSuperContent(strContentClass)
    } else implementation.addSuperinterface(contentClass)
    group.inheritedGroups.map { generateTagGroup(it).typeName() }.distinctBy { it.toString() }.forEach { group ->
        contentInterface.addSuperinterface(group.parameterizedBy(tagGeneric))
        implementation.addSuperContent(group)
    }
    superTypes.forEach { superType ->
        contentInterface.addSuperinterface(superType)
        implementation.addSuperContent(superType)
    }
    val incompleteSpec = contentInterface.build().asFile(packageName ?: "tags.content", fileName)
    tagGroups[group.name] = incompleteSpec
    implementation.addSuperinterface(incompleteSpec.typeName().parameterizedBy(tagGeneric))

    val inheritedFromMultiple = group.inheritedGroups
        .flatMap { it.tags }
        .groupingBy { it }.eachCount()
        .filter { it.value > 1 }
        .keys

    inheritedFromMultiple.forEach {
        val tag = getTag(it).tagInterface
        val delegate = PropertySpec.builder(it.name, tag.typeName().parameterizedBy(tagGeneric))
            .addModifiers(KModifier.ABSTRACT, KModifier.OVERRIDE)
        contentInterface.addProperty(delegate.build())
        val delegateImpl = PropertySpec.builder(it.name, tag.typeName().parameterizedBy(tagGeneric))
            .addModifiers(KModifier.OVERRIDE)
            .getter(
                FunSpec.getterBuilder().addCode("TODO(\"Extension functions handle tag creation\")")
                    .build()
            )
        implementation.addProperty(delegateImpl.build())
    }

    group.directTags.forEach {
        val tag = getTag(it)
        contentInterface.addSuperinterface(tag.tagInterface.typeName().parameterizedBy(tagGeneric))
        val delegateImpl = PropertySpec.builder(it.name, tag.tagInterface.typeName().parameterizedBy(tagGeneric))
            .addModifiers(KModifier.OVERRIDE)
            .getter(
                FunSpec.getterBuilder()
                    .addCode("TODO(\"Extension functions handle tag creation\")")
                    .build()
            )
        implementation.addProperty(delegateImpl.build())
    }

    contentInterface.addType(implementation.build())
    val completeSpec = contentInterface.build().asFile(packageName ?: "tags.content", fileName)
    tagGroups[group.name] = completeSpec
    return completeSpec
}

fun TypeSpec.Builder.addSuperContent(superType: ClassName): TypeSpec.Builder {
    return addSuperinterface(superType.parameterizedBy(tagGeneric))
}

fun <K, V> MutableMap<K, V>.nonConcurrentComputeIfAbsent(key: K, defaultValue: () -> V): V {
    this[key]?.let { return it }
    val value: V = defaultValue()
    this[key] = value
    return value
}