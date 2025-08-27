package eu.niton.ktx.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSFile
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec.Companion.anonymousClassBuilder
import eu.niton.ktx.Content
import eu.niton.ktx.KtxElement
import eu.niton.ktx.RenderableContent
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
    val bodyTypeAlias: FileContent.TypeAlias?,
    val contentTypeAlias: FileContent.TypeAlias?
) {
    val fileContent = listOfNotNull(tagInterface, tagFunction, bodyTypeAlias,contentTypeAlias)
}

private val tags = mutableMapOf<String, TagFile>()
private val tagGroups = mutableMapOf<String, TagGroupFile>()

fun Generator.generate() {
    htmlSchema.value.tags.forEach {
        getTag(it.value)
    }
    val tagFileContent = tags.values.flatMap { it.fileContent }
    val groupFileContent = tagGroups.values.flatMap { it.fileContent }
    val files = (tagFileContent + groupFileContent + attributeTypes.values)
        .groupBy { it.packageName }
        .mapValues { it.value.groupBy { it.fileName } }

    files.forEach { (packageName, files) ->
        files.forEach { (fileName, contents) ->
            writeFile(fileName, packageName, contents)
        }
    }
}

private val tagGeneric = TypeVariableName("T", RenderableContent::class.asClassName())
private val strContentClass = StringContent::class.asClassName()
private val contentClass = Content::class.asClassName()
private val renderable = RenderableContent::class.asClassName()
private val contentImpl = ClassName("eu.niton.ktx", "DefaultContent")
private val renderFn = MemberName("eu.niton.ktx", "render")
private val tagFn = MemberName("eu.niton.ktx", "tag")
fun TypeSpec.Builder.addSelfReferentialTypeVar(name: String): TypeSpec.Builder {
    return addTypeVariable(
        tagGeneric.copy(
            bounds = listOf(
                ClassName.bestGuess(name).parameterizedBy(tagGeneric),
                renderable
            )
        )
    )
}

fun Generator.getTag(tag: TagInfo): TagFile {
    return tags.nonConcurrentComputeIfAbsent(tag.name) {
        // Create the per-tag interface (e.g., DivHtmlTag<T> : ContentDefinition<T>)
        val className = tag.className()
        val interfaceClassName = "eu.niton.ktx.tags.$className"


        val tagInterface = TypeSpec.interfaceBuilder(className)
            .addSelfReferentialTypeVar(className)
            .addSuperinterface(contentClass.parameterizedBy(tagGeneric))
            .build()
            .asFile("tags")

        val contentType = getContentType(tag.content, tag.name + "TagContent", filename = className)?.let {
            it.component1().parameterizedBy(STAR) to it.component2()
        }
        val contentTypeAlias = contentType?.let {
            TypeAliasSpec.builder("${tag.name.humanize().capitalize()}Content", it.component1())
                .build().asFile("tags", className) to it.component2()
        }
        val bodyTypeAlias = contentTypeAlias?.let {
            val contentFnType = LambdaTypeName.get(receiver = it.component1().typeName(), returnType = UNIT)
            TypeAliasSpec.builder(tag.name.humanize().capitalize() + "Body", contentFnType)
                .build().asFile("tags", className) to it.component2()
        }
        val bodyType = bodyTypeAlias?.let { it.component1().typeName() to it.component2() }
        // Create the extension function that calls tag()
        val tagFunction = generateTagFunction(tag, bodyType)
            .receiver(tagInterface.typeName().parameterizedBy(STAR))
            .build()
            .asFile("tags", className)

        TagFile(tagInterface, tagFunction, bodyTypeAlias?.component1(), contentTypeAlias?.component1())
    }
}

// Generate the extension function for a tag that calls the new runtime API
private fun Generator.generateTagFunction(tag: TagInfo, bodyType: Pair<TypeName, MemberName>?): FunSpec.Builder {
    val tagName = tag.name
    val className = tag.className()
    val funBuilder = FunSpec.builder(tagName)
        .addModifiers(KModifier.INLINE)

    // Use existing logic to add attribute parameters
    tag.content.allAttributes.sortedByDescending { it.required }.forEach { attribute ->
        funBuilder.addParameter(
            getAttributeParameter(attribute, tag)
                .addModifiers(KModifier.NOINLINE)
                .build()
        )
    }

    val bodyParam = bodyType?.component1()?.let {
        val param = ParameterSpec.builder("body", it).build()
        funBuilder.addParameter(param)
        param
    }

    // Generate function body that calls tag() with new runtime API
    val attributeMapCode = getAttributeMap(tag)
    val eventMapCode = getEventListenerMap(tag)
    val bodyExpression = if(bodyType != null && bodyParam != null) {
        CodeBlock.of("%M(%N)", bodyType.component2(), bodyParam.name)
    } else CodeBlock.of("null")

    funBuilder.addStatement(
        "return %M(%S, %L, %L, %L)",
        tagFn,
        tagName,
        attributeMapCode,
        eventMapCode,
        bodyExpression
    )

    return funBuilder
}


private fun getEventListenerMap(tag: TagInfo): CodeBlock {
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

private fun Generator.getAttributeParameter(
    attribute: AttributeInfo,
    tag: TagInfo
): ParameterSpec.Builder {
    val qualifiedName = attribute.fullyQualifiedName(tag)
    val attributeType = getAttributeType(
        attribute.name,
        qualifiedName,
        attribute.type
    ).copy(nullable = !attribute.required)
    val type = attributeType as? LambdaTypeName ?: LambdaTypeName.get(returnType = attributeType)
        .copy(nullable = !attribute.required)
    val param = ParameterSpec.builder(attribute.name.humanize(), type)
    if (!attribute.required) {
        val defaultValue = if (attribute.defaultValue != null) {
            CodeBlock.builder().beginControlFlow("").add(
                getAttributeLiteral(attribute.name, qualifiedName, attribute.type, attribute.defaultValue)
            ).endControlFlow().build()
        } else CodeBlock.of("null")
        param.defaultValue(defaultValue)
    }
    return param
}

//Isn't this the wrong way around??
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

data class TagGroupFile(
    val groupInterface: FileContent.Type,
    val implementation: FileContent.Type,
    val renderFunction: FileContent.Function
) {
    val fileContent = listOf(groupInterface, implementation, renderFunction)
}

fun Generator.generateTagGroup(
    group: TagGroup,
    packageName: String? = null,
    fileName: String? = null,
    allowText: Boolean = false,
    superTypes: Collection<ClassName> = emptyList()
): TagGroupFile {
    tagGroups[group.name]?.let {
        return it
    }

    val interfaceName = group.name.humanize().capitalize()
//        .replace("Tag", "")
        .replace("Content", "")
        .replace("Group", "") + "Content"
    val contentInterface = TypeSpec.interfaceBuilder(interfaceName)
        .addSelfReferentialTypeVar(interfaceName)
        .addSuperinterface(
            if (allowText) strContentClass.parameterizedBy(tagGeneric)
            else contentClass.parameterizedBy(tagGeneric)
        )
    val implName = "${interfaceName}Impl"
    val implementation = TypeSpec.classBuilder(implName)
        .addModifiers(KModifier.INTERNAL)
        .addAnnotation(PublishedApi::class)
        .superclass(contentImpl.parameterizedBy(ClassName.bestGuess(implName)))
        .addSuperclassConstructorParameter("::${implName}")
    group.inheritedGroups.map { generateTagGroup(it).groupInterface.typeName() }.distinctBy { it.toString() }
        .forEach { group ->
            contentInterface.addSuperinterface(group.parameterizedBy(tagGeneric))
        }
    superTypes.forEach { superType ->
        contentInterface.addSuperinterface(superType)
    }
    val incompleteSpec = contentInterface.build().asFile(packageName ?: "tags.content", fileName)
    val incompleteImpl = implementation.build().asFile(packageName ?: "tags.content", fileName)
    val renderFn = FunSpec.builder("render")
        .addModifiers(KModifier.INLINE)
        .addParameter(
            ParameterSpec(
                "body", LambdaTypeName.get(
                    receiver = incompleteSpec.typeName().parameterizedBy(STAR),
                    returnType = UNIT
                )
            )
        )
        .returns(KtxElement::class.asClassName().copy(nullable = true))
        .addStatement("return %M(%L,body)", renderFn, incompleteImpl.typeName().constructorReference())
        .build()
        .asFile(packageName ?: "tags.content", fileName ?: incompleteSpec.fileName)
    tagGroups[group.name] = TagGroupFile(
        groupInterface = incompleteSpec,
        implementation = incompleteImpl,
        renderFn
    )
    implementation.addSuperinterface(incompleteSpec.typeName().parameterizedBy(ClassName.bestGuess(implName)))

    group.directTags.forEach {
        val tag = getTag(it)
        contentInterface.addSuperinterface(tag.tagInterface.typeName().parameterizedBy(tagGeneric))
    }

    val completeFile = run {
        val completeSpec = contentInterface.build().asFile(packageName ?: "tags.content", fileName)
        val completeImpl = implementation.build().asFile(packageName ?: "tags.content", fileName?:completeSpec.fileName)
        TagGroupFile(
            groupInterface = completeSpec,
            implementation = completeImpl,
            renderFunction = renderFn
        )
    }
    tagGroups[group.name] = completeFile
    return completeFile
}


/**
 * Generates the interface of the "Body-receiver" the type that is given to the body-lambda
 */
private fun Generator.getContentType(content: TagContent, fallbackName: String, filename: String? = null): Pair<ClassName,MemberName>? {
    val requiredSuperinterfaces = content.childrenGroups.size +
            (if (content.allowText) 1 else 0) +
            (if (content.superType != null) 1 else 0)
    if (content.directChildren.isNotEmpty() || requiredSuperinterfaces > 1) {
        val tagGroup = TagGroup(
            content.name ?: fallbackName,
            content.directChildren.toList(),
            content.childrenGroups
        );
        val superType = listOfNotNull(content.superType?.let { getContentType(it, fallbackName)?.component1() })
        val packageName = if (content.name != null) "tags.content" else "tags"
        val fileName = if (content.name != null) null else filename
        return generateTagGroup(
            tagGroup,
            packageName,
            fileName = fileName,
            allowText = content.allowText,
            superTypes = superType
        ).let {
            it.groupInterface.typeName() to it.renderFunction.reference()
        }
    } else if (content.allowText) {
        return strContentClass to renderFn
    } else if (content.superType != null) {
        return getContentType(content.superType!!, fallbackName)
    } else if (content.childrenGroups.size == 1) {
        return generateTagGroup(content.childrenGroups.first()).let {
            it.groupInterface.typeName() to it.renderFunction.reference()
        }
    } else return null
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