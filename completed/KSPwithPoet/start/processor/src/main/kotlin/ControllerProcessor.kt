package fr.esgi

import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
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
        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            val className = classDeclaration.simpleName.asString()
            val classNameLower = className.replaceFirstChar { it.lowercase() }
            val path = classDeclaration.annotations.find { it.shortName.asString() == "CreateBasicController"}?.arguments?.first()?.value as String

            val restControllerAnnotation = AnnotationSpec.builder(ClassName("org.springframework.web.bind.annotation", "RestController"))
                .build()
            val requestMappingAnnotation = AnnotationSpec.builder(ClassName("org.springframework.web.bind.annotation", "RequestMapping"))
                .addMember("%S", path)
                .build()

            val createdResponseAnnotation = AnnotationSpec.builder(ClassName("org.springframework.web.bind.annotation", "ResponseStatus"))
                .addMember("value = %T.CREATED", ClassName("org.springframework.http", "HttpStatus"))
                .build()


            val requestBodyAnnotation = AnnotationSpec.builder(ClassName("org.springframework.web.bind.annotation", "RequestBody"))
                .build()
            val pathVariable = AnnotationSpec.builder(ClassName("org.springframework.web.bind.annotation", "PathVariable"))
                .build()

            val postMappingAnnotation = AnnotationSpec.builder(ClassName("org.springframework.web.bind.annotation", "PostMapping"))
                .build()
            val getMappingAnnotation = AnnotationSpec.builder(ClassName("org.springframework.web.bind.annotation", "GetMapping"))
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

        private fun visitPropertyDeclaration(property: KSPropertyDeclaration, className: String, classNameLower: String, repositoryClassNameLower: String) {
            // Implementation for visiting property declarations
        }
    }
}