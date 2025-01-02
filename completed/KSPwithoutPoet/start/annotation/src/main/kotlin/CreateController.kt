package fr.esgi

@Target(AnnotationTarget.CLASS)
annotation class CreateBasicController(val path: String = " ")

@Target(AnnotationTarget.FIELD)
annotation class EntityId()