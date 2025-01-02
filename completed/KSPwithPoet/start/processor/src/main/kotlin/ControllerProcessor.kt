package fr.esgi

import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import java.io.OutputStream

class ControllerProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(CreateBasicController::class.qualifiedName.toString())
            .filterIsInstance<KSClassDeclaration>()

        if (!symbols.iterator().hasNext()) {
            logger.warn("No other classes found with @CreateBasicController.")
            return emptyList()
        }

        symbols.forEach { classDeclaration ->
            if (!classDeclaration.validate()) return@forEach
            logger.warn("class name: $classDeclaration")
            classDeclaration.accept(ControllerVisitor(), Unit)
        }

        return symbols.filterNot { it.validate() }.toList()

    }

    inner class ControllerVisitor() : KSVisitorVoid() {

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


        private val createdResponseAnnotation = AnnotationSpec.builder(ClassName("org.springframework.web.bind.annotation", "ResponseStatus"))
            .addMember("value = %T.CREATED", ClassName("org.springframework.http", "HttpStatus"))
            .build()

        private val requestBodyAnnotation = AnnotationSpec.builder(ClassName("org.springframework.web.bind.annotation", "RequestBody"))
            .build()
        private val pathVariable = AnnotationSpec.builder(ClassName("org.springframework.web.bind.annotation", "PathVariable"))
            .build()
        private val postMappingAnnotation = AnnotationSpec.builder(ClassName("org.springframework.web.bind.annotation", "PostMapping"))
            .build()
        private val getMappingAnnotation = AnnotationSpec.builder(ClassName("org.springframework.web.bind.annotation", "GetMapping"))
            .build()

        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {

            val className = classDeclaration.simpleName.asString()
            val classNameLower = className.replaceFirstChar { it.lowercase() }
            val path = classDeclaration.annotations.find { it.shortName.asString() == "CreateBasicController"}?.arguments?.first()?.value as String

            val requestMappingAnnotation = AnnotationSpec.builder(ClassName("org.springframework.web.bind.annotation", "RequestMapping"))
                .addMember("%S", path)
                .build()
            val restControllerAnnotation = AnnotationSpec.builder(ClassName("org.springframework.web.bind.annotation", "RestController"))
                .build()

            val repositoryClass = ClassName("fr.esgi", className + "Repository")
            val repositoryClassNameLower = repositoryClass.simpleName.replaceFirstChar { it.lowercase() }
            val repositoryParam = ParameterSpec.builder(repositoryClassNameLower, repositoryClass)
                .build()

            val controllerClassBuilder = TypeSpec.classBuilder("${className}Controller")
                .addAnnotation(restControllerAnnotation)
                .addAnnotation(requestMappingAnnotation)
                .primaryConstructor(
                    FunSpec.constructorBuilder()
                        .addParameter(repositoryParam)
                        .build()
                )
                .addProperty(
                    PropertySpec.builder(repositoryClassNameLower, repositoryClass)
                        .addModifiers(KModifier.PRIVATE)
                        .initializer(repositoryClassNameLower)
                        .build()
                )
                .addFunction(
                    FunSpec.builder("create${className}")
                        .addAnnotation(postMappingAnnotation)
                        .addAnnotation(createdResponseAnnotation)
                        .addModifiers(KModifier.PUBLIC)
                        .addParameter(
                            ParameterSpec.builder(classNameLower, ClassName("fr.esgi", className))
                                .addAnnotation(requestBodyAnnotation)
                                .build())
                        .addStatement("return ${repositoryClassNameLower}.save(${classNameLower})")
                        .returns(ClassName("fr.esgi", className))
                        .build()
                )
                .addFunction(
                    FunSpec.builder("getAll${className}s")
                        .addAnnotation(getMappingAnnotation)
                        .addModifiers(KModifier.PUBLIC)
                        .addStatement("return ${repositoryClassNameLower}.findAll()")
                        .returns(List::class.asClassName().parameterizedBy(ClassName("fr.esgi", className)))
                        .build()
                )

            classDeclaration.getDeclaredProperties().forEach { property ->
                visitPropertyDeclaration(property,controllerClassBuilder, classNameLower, repositoryClassNameLower)
            }


            val fileSpec = FileSpec.builder("fr.esgi.generated", className + "Controller")
                .addType(controllerClassBuilder.build())
                .build()

            val file = codeGenerator.createNewFile(
                Dependencies(false, classDeclaration.containingFile!!),
                "fr.esgi.generated",
                className + "Controller"
            )

            file.bufferedWriter().use { writer ->
                fileSpec.writeTo(writer)
            }
        }

        private fun visitPropertyDeclaration(property: KSPropertyDeclaration, controllerClassBuilder: TypeSpec.Builder, classNameLower: String, repositoryClassNameLower: String) {

            val className = classNameLower.replaceFirstChar { it.uppercase() }

            val isAnnotated = property.annotations.any {
                it.shortName.getShortName() == "Id"
            }
            if(isAnnotated) {
                val propName = property.simpleName.asString()
                val propType = property.type.resolve()

                val getMappingAnnotationWithArgs = AnnotationSpec.builder(ClassName("org.springframework.web.bind.annotation", "GetMapping"))
                    .addMember("%S", "/{$propName}")
                    .build()
                controllerClassBuilder.addFunction(
                    FunSpec.builder("get${className}By${propName.replaceFirstChar { it.uppercase() }}")
                        .addAnnotation(getMappingAnnotationWithArgs)
                        .addModifiers(KModifier.PUBLIC)
                        .addParameter(
                            ParameterSpec.builder(propName, resolveType(propType))
                                .addAnnotation(pathVariable)
                                .build())
                        .addStatement("return ${repositoryClassNameLower}.findById(${propName}).orElse(null)")
                        .returns(ClassName("fr.esgi", className))
                        .build()
                )


            }
        }
    }
}