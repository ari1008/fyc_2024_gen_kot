package fr.esgi

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

class DTOProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        logger.warn("DTOProcessor started processing.")

        // Récupérer toutes les classes annotées avec DTOAnnotation
        val symbols = resolver.getSymbolsWithAnnotation(DTOAnnotation::class.qualifiedName.toString())
            .filterIsInstance<KSClassDeclaration>()

        if (!symbols.iterator().hasNext()) {
            logger.warn("No other classes found with @DTOAnnotation.")
            return emptyList()
        }

        // Parcourir chaque classe annotée
        symbols.forEach { classDeclaration ->
            if (!classDeclaration.validate()) return@forEach
            logger.warn("class name: $classDeclaration")
            classDeclaration.accept(DTOVisitor(), Unit)
        }


        return symbols.filterNot { it.validate() }.toList()
    }

    inner class DTOVisitor : KSVisitorVoid() {

        private fun resolveType(ksType: KSType): TypeName {
            val qualifiedName = ksType.declaration.qualifiedName?.asString()

            if (qualifiedName != null && ksType.arguments.isEmpty()) {
                return when (qualifiedName) {
                    "kotlin.Int" -> ClassName("kotlin", "Int")
                    "kotlin.String" -> ClassName("kotlin", "String")
                    "kotlin.Boolean" -> ClassName("kotlin", "Boolean")
                    "kotlin.Float" -> ClassName("kotlin", "Float")
                    "kotlin.Double" -> ClassName("kotlin", "Double")
                    "kotlin.Unit" -> ClassName("kotlin", "Unit")
                    else -> ClassName.bestGuess(qualifiedName)
                }
            }

            if (qualifiedName != null) {
                val rawType = ClassName.bestGuess(qualifiedName)

                val typeArguments = ksType.arguments.map { argument ->
                    val resolvedType = argument.type?.resolve()
                    if (resolvedType != null) {
                        resolveType(resolvedType)
                    } else {
                        ClassName("kotlin", "Any")
                    }
                }

                return rawType.parameterizedBy(typeArguments)
            }

            return ClassName("kotlin", "Any")
        }


        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            val className = classDeclaration.simpleName.asString()

            val classBuilder = TypeSpec.classBuilder(className + "DTO")
                .addModifiers(KModifier.DATA)

            val constructorBuilder = FunSpec.constructorBuilder()

            classDeclaration.getAllProperties().forEach { propertyDeclaration ->

                val propertyName = propertyDeclaration.simpleName.asString()

                val propertyType = resolveType(propertyDeclaration.type.resolve())

                val parameterSpec = ParameterSpec.builder(propertyName, propertyType).build()
                constructorBuilder.addParameter(parameterSpec)

                val propertySpec = PropertySpec.builder(propertyName, propertyType)
                    .initializer(propertyName)
                    .build()
                classBuilder.addProperty(propertySpec)
            }

            classBuilder.primaryConstructor(constructorBuilder.build())

            val fileSpec = FileSpec.builder("fr.esgi.generated.DTOs", className + "DTO")
                .addType(classBuilder.build())
                .build()

            val file = codeGenerator.createNewFile(
                Dependencies(false, classDeclaration.containingFile!!),
                "fr.esgi.generated.DTOs",
                className + "DTO"
            )

            file.bufferedWriter().use { writer ->
                fileSpec.writeTo(writer)
            }
        }

    }
}
