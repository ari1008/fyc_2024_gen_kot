package fr.esgi



@DTOAnnotation
data class Restaurant(
    @RemoveFromDTO
    val id : Int,
    @RemoveFromDTO
    val ownerId : String,
    val ownerName : String,
    val restaurantName : String,
    val location : String,
    val reviews  : Int
)
fun main() {
    println("Hello, World!")
    println("finished")
}
