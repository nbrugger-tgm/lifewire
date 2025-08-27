package eu.niton.ktx.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.visitor.KSEmptyVisitor


val SCHEME_URL = "html_5.xsd".asResourceUrl()
class Processor(private val env: SymbolProcessorEnvironment) : SymbolProcessor {
    @OptIn(KspExperimental::class)
    private val visitor: KSEmptyVisitor<Unit, Unit> = object : KSEmptyVisitor<Unit,Unit>() {
        override fun defaultHandler(node: KSNode, data: Unit) {
            if(node !is KSFile) return;
            val annotation = node.getAnnotationsByType(GenerateKtx::class).firstOrNull()
            if(annotation != null) {
                Generator(node.packageName.asString(), env.codeGenerator, env.logger, node, SCHEME_URL).generate()
            }
        }
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver.getNewFiles().forEach { it.accept(visitor, Unit) }
        return emptyList()
    }
}