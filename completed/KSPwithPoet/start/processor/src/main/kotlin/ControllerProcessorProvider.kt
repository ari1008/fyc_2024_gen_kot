package fr.esgi

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class ControllerProcessorProvider : SymbolProcessorProvider{
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return ControllerProcessor(
            codeGenerator = environment.codeGenerator,
            logger = environment.logger
        )
    }
}