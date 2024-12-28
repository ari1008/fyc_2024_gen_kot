import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated


class MapperProcessor(
    private val logger: KSPLogger,
    private val codeGenerator: CodeGenerator,
): SymbolProcessor{
    override fun process(resolver: Resolver): List<KSAnnotated> {
        logger.warn(resolver.toString())
        return emptyList()
    }

}