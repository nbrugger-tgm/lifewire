package eu.niton.ktx.processor

import com.sun.xml.xsom.*
import com.sun.xml.xsom.parser.XSOMParser
import org.xml.sax.ErrorHandler
import org.xml.sax.SAXParseException
import java.net.URL
import javax.xml.parsers.SAXParserFactory

const val HTML_NAMESPACE = "html-5"

private val namedBooleans: Map<String, Boolean> = mapOf(
    "on" to true,
    "off" to false,
    "enabled" to true,
    "disabled" to false
)

private val xsdToType = mapOf(
    "boolean" to AttributeType.Boolean,
    "string" to AttributeType.String,
    "anyURI" to AttributeType.URI,
    "float" to AttributeType.Number,
    "double" to AttributeType.Number
)

private class XsdParser {
    val globalTags = HashMap<String, TagInfo>()
    val globalAttributeGroups = mutableMapOf<String, AttributeGroup>()
    val globalTagGroups = mutableMapOf<String, TagGroup>()
    val globalTagContentTypes = mutableMapOf<String, TagContent>()

    private fun parseAttribute(attribute: XSAttributeUse): AttributeInfo {
        val name = attribute.decl.name
        val type = attribute.decl.type
        val global = attribute.decl.isGlobal
        val parsedType = parseAttributeType(type, name)
        return AttributeInfo(
            name, parsedType,
            required = attribute.isRequired,
            defaultValue = attribute.defaultValue?.value,
            fixedValue = attribute.fixedValue?.value,
            global = global
        )
    }

    private fun parseAttributeType(type: XSSimpleType, attributeName: String): AttributeType {
        val name = if(type.isGlobal && type.name != null) type.name else attributeName
        return if (type.isUnion) {
            val enumEntries = type.asUnion()
                .asSequence()
                .filter { it.isRestriction }
                .map { it.asRestriction() }
                .flatMap { it.declaredFacets ?: emptyList() }
                .filter { it.name == "enumeration" }
                .map { it.value.value }
                .toList()
            AttributeType.Enum(name, enumEntries, AttributeType.String,type.isGlobal)
        } else if (type.name == "functionBody") {
            AttributeType.EventHandler
        } else if (type.isPrimitive || type.name in setOf<String?>("integer", "string", "boolean", "decimal", "float", "double")) {
            xsdToType[type.primitiveType.name] ?: AttributeType.String
        } else if (type.isRestriction) {
            val restriction = type.asRestriction()
            val enumEntries = restriction.declaredFacets
                .filter { it.name == "enumeration" }
                .map { it.value.value }

            if (enumEntries.filter { !it.isEmpty() }.size == 1 && enumEntries.single { !it.isEmpty() } == attributeName) {
                return AttributeType.Toggle
            }
            if(enumEntries.size == 3 && enumEntries.any { it.isEmpty() }) {
                val realEntries = enumEntries.filter { !it.isEmpty() }
                if(realEntries.containsAll(setOf("true", "false"))) return AttributeType.Boolean
                val namedBool = parseBooleanEnum(realEntries, type.isGlobal)
                if(namedBool != null) return namedBool
            }
            val namedBool = parseBooleanEnum(enumEntries, type.isGlobal)
            namedBool ?: if (enumEntries.isEmpty()) {
                AttributeType.String
            } else {
                AttributeType.Enum(
                    name,
                    enumEntries,
                    AttributeType.String,
                    type.isGlobal
                )
            }
        }  else {
            AttributeType.String
        }
    }

    private fun isEventHandler(name: String): Boolean = name.length >= 3 && name.startsWith("on")

    private fun parseBooleanEnum(enumEntries: List<String>, global: Boolean): AttributeType.NamedBoolean? {
        if (enumEntries.size != 2) return null
        val bool1 = namedBooleans[enumEntries[0]]
        val bool2 = namedBooleans[enumEntries[1]]
        //both are named booleans and don't describe the same bool
        return if (bool1 == null || bool2 == null || bool1 == bool2) null;
        else if (bool1) AttributeType.NamedBoolean(enumEntries[0], enumEntries[1],global)
        else AttributeType.NamedBoolean(enumEntries[1], enumEntries[0],global)
    }

    private fun parseAttributeGroup(attributeGroup: XSAttGroupDecl): AttributeGroup {
        if(attributeGroup.isGlobal) globalAttributeGroups[attributeGroup.name]?.let { return it }
        val attributes = attributeGroup.declaredAttributeUses.map { parseAttribute(it) }
        val includedGroups = attributeGroup.attGroups.map { parseAttributeGroup(it) }
        AttributeGroup(attributeGroup.name, attributes, includedGroups).let {
            if(attributeGroup.isGlobal) globalAttributeGroups[it.name] = it
            return it
        }
    }


    fun parseElement(elementDeclaration: XSElementDecl): TagInfo {
        val name = elementDeclaration.name
        if(elementDeclaration.isGlobal) globalTags[name]?.let { return it }
        val type = elementDeclaration.type
        if (!type.isComplexType) {
            throw IllegalStateException("Element type ${elementDeclaration.name} is not complex type")
        }

        val contentRef = TagContentHolder()
        val tag = TagInfo(name = name, contentProvider = contentRef)
        if(elementDeclaration.isGlobal) globalTags[name] = tag
        contentRef.tagContent = parseContentType(type.asComplexType())
        return tag
    }

    private fun parseContentType(
        complex: XSComplexType
    ): TagContent {
        val canBeReferenced = complex.name != null && complex.isGlobal
        if(canBeReferenced) globalTagContentTypes[complex.name]?.let { return it }

        //This assumes attributes & their groups are acyclic
        val attributeGroups = complex.attGroups.map { parseAttributeGroup(it) }
        val attributes = complex.declaredAttributeUses.map { parseAttribute(it) }
        val baseType = complex.baseType.let { if(it.name == "anyType") null else it }


        val tagGroupReferences = mutableListOf<TagGroup>()
        val directChildren = mutableListOf<TagInfo>()
        val superTypeReference = TagContentHolder()
        val content = TagContent(
            name = complex.name,
            abstract = complex.isAbstract,
            directChildren = directChildren,
            childrenGroups = tagGroupReferences,
            futureSuperTag = superTypeReference,
            attributeGroups = attributeGroups,
            directAttributes = attributes,
            allowText = complex.isMixed
        )
        if(canBeReferenced) globalTagContentTypes[complex.name] = content

        superTypeReference.tagContent = baseType?.let {
            if (it.isComplexType) parseContentType(baseType.asComplexType())
            else throw IllegalStateException("Base type (${it.name}) of ${complex.name} is not complex type")
        }

        val contentTerm = (if(baseType != null) complex.explicitContent else complex.contentType)?.asParticle()?.term
        if (contentTerm != null) {
            fun parseContent(term: XSTerm) {
                if (term.isModelGroup) {
                    term.asModelGroup().children.forEach { parseContent(it.term) }
                } else if (term.isElementDecl) {
                    if(directChildren.none { it.name == term.asElementDecl().name })
                        directChildren.add(parseElement(term.asElementDecl()))
                } else if (term.isModelGroupDecl) {
                    tagGroupReferences.add(parseTagGroup(term.asModelGroupDecl()))
                } else throw IllegalStateException("Tag Content ${complex.name} has non-supported content type $term")
            }
            parseContent(contentTerm)
        }

        return content
    }

    private fun parseTagGroup(modelGroupDeclaration: XSModelGroupDecl): TagGroup {
        val name = modelGroupDeclaration.name
        if(modelGroupDeclaration.isGlobal) globalTagGroups[name]?.let { return it }
        val childTags = mutableListOf<TagInfo>()
        val includedGroups = mutableListOf<TagGroup>()
        val group = TagGroup(name, childTags, includedGroups)
        if(modelGroupDeclaration.isGlobal) globalTagGroups[name] = group

        val (tagChildren, nonTagChildren) = modelGroupDeclaration.modelGroup.children
            .map { it.term }
            .partition { it.isElementDecl }
        val (groupChildren, unknownChildren) = nonTagChildren.partition { it.isModelGroupDecl }
        if (unknownChildren.isNotEmpty()) {
            throw IllegalStateException("TagGroup $name has invalid (neither element nor model group) children (${unknownChildren})")
        }
        childTags.addAll(tagChildren.map { parseElement(it.asElementDecl()) })
        includedGroups.addAll(groupChildren.map { parseTagGroup(it.asModelGroupDecl()) })
        return group
    }

}

fun parseXmlSchema(xsd: URL): XmlSchema {
    val parser = XsdParser()
    val schema = readXsd(xsd)

    val xmlSchema = XmlSchema()
    schema.elementDecls.values.forEach { elementDeclaration ->
        xmlSchema.tags[elementDeclaration.name] = parser.parseElement(elementDeclaration)
    }
    return xmlSchema
}

private fun readXsd(xsd: URL): XSSchema {
    val xsomParser = XSOMParser(SAXParserFactory.newInstance())
    xsomParser.errorHandler = object : ErrorHandler {
        override fun warning(exception: SAXParseException?) {
            exception?.printStackTrace()
        }

        override fun error(exception: SAXParseException?) {
            exception?.printStackTrace()
        }

        override fun fatalError(exception: SAXParseException?) {
            exception?.printStackTrace()
        }
    }
    xsomParser.parse(xsd)
    return xsomParser.result.getSchema(HTML_NAMESPACE)
}

