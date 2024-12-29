package fr.esgi



@DTOAnnotation
class Restaurant(
    @RemoveFromDTO
    val id : Int,
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
