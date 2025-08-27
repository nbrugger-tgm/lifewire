package eu.niton.ktx.processor

import java.util.*

class XmlSchema {
    val tags = TreeMap<String, TagInfo>()
}

data class AttributeGroup(
    val name: String,
    val directAttributes: List<AttributeInfo>,
    val inheritedGroups: List<AttributeGroup>
) {
    val className = name.capitalize() + "Group"
    val attributes: List<AttributeInfo> = directAttributes + inheritedGroups.flatMap { it.attributes }
    val parents = if (inheritedGroups.isNotEmpty()) inheritedGroups.map { it.className } else listOf("Tag")
    val attributeNames = attributes.map { it.name }.toSet()
}

sealed interface AttributeType {
    object Boolean : AttributeType
    object Toggle : AttributeType
    object Number : AttributeType
    object String : AttributeType
    object URI : AttributeType
    object EventHandler: AttributeType

    data class NamedBoolean(val trueName: kotlin.String, val falseName: kotlin.String, val global: kotlin.Boolean) : AttributeType
    data class Enum(val name: kotlin.String?, val values: List<kotlin.String>, val type: AttributeType, val global: kotlin.Boolean) : AttributeType
}
interface HasType {
    val type: AttributeType
}

data class AttributeInfo(
    val name: String,
    override val type: AttributeType,
    val required: Boolean,
    val defaultValue: String?,
    val fixedValue: String?,
    val global: Boolean
) : HasType {
    init {
        if(defaultValue != null && fixedValue != null && defaultValue != fixedValue)
            throw IllegalArgumentException("Attribute (${name} cannot have differing defaultValue and fixedValue (default: $defaultValue, fixed: $fixedValue)")
    }
}
data class TagContentHolder(var tagContent: TagContent? = null)
class TagContent(
    val name: String?,
    val directChildren: Collection<TagInfo>,
    val directAttributes: Collection<AttributeInfo>,
    val childrenGroups: Collection<TagGroup>,
    val attributeGroups: Collection<AttributeGroup>,
    val allowText: Boolean,
    val abstract: Boolean,
    private val futureSuperTag: TagContentHolder
){
    val superType: TagContent? get() = futureSuperTag.tagContent
    private val allAttributesLazy:Lazy<List<AttributeInfo>> = lazy {
        val map: MutableMap<String, AttributeInfo> = HashMap()
        map.putAll(directAttributes.map { it.name to it })
        attributeGroups.flatMap { it.attributes }.forEach {
            map.putIfAbsent(it.name, it)
        }
        superType?.allAttributes?.forEach {
            map.putIfAbsent(it.name, it)
        }
        return@lazy map.values.toList()
    }
    val allAttributes get() = allAttributesLazy.value
    val possibleChildren: Collection<TagInfo> = directChildren + childrenGroups.flatMap { it.tags }
}

data class TagInfo(
    val name: String,
    private val contentProvider: TagContentHolder
) {
    val content get() = contentProvider.tagContent?:throw IllegalStateException("Tag $name was not fully processed and has no content")
}

data class TagGroup(
    val name: String,
    val directTags: Collection<TagInfo>,
    val inheritedGroups: Collection<TagGroup>
) {
    val tags:Collection<TagInfo> = directTags + inheritedGroups.flatMap { it.tags }
}
