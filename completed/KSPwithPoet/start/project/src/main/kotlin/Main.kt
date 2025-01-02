package fr.esgi


import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Entity
@CreateBasicController(path = "api/vehicles")
data class Vehicle(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val brand: String,
    val model: String,
    val vehicleYear: Int
) {
    constructor() : this(0, "", "", 0) {

    }
}

fun main() {
    println("Hello, World!")
    println("finished")
}
