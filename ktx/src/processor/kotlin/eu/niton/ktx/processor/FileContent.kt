package eu.niton.ktx.processor

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.TypeAliasSpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec

sealed interface FileContent {
    val fileName: String
    val packageName: String

    data class Type(
        val type: TypeSpec,
        override val fileName: String,
        override val packageName: String
    ) : FileContent {
        fun typeName(): ClassName {
            val name = type.name ?: throw IllegalStateException("Type without name cannot be referenced: $type")
            return ClassName(packageName, name)
        }
    }

    data class Function(
        val function: FunSpec,
        override val fileName: String,
        override val packageName: String
    ) : FileContent {
        fun reference(): MemberName {
            return MemberName(
                packageName = packageName,
                simpleName = function.name
            )
        }
    }

    class TypeAlias(
        val spec: TypeAliasSpec,
        override val fileName: String,
        override val packageName: String
    ) : FileContent {
        fun typeName() = ClassName(
            packageName = packageName,
            spec.name
        )
    }
}