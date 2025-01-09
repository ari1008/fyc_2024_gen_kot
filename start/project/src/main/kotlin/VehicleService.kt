package fr.esgi

import org.springframework.stereotype.Service
import java.util.*

@Service
class VehicleService(private val vehicleRepository: VehicleRepository) {

    fun getAllVehicles(): List<Vehicle> {
        return vehicleRepository.findAll()
    }

    fun createVehicle(vehicle: Vehicle): Vehicle {
        return vehicleRepository.save(vehicle)
    }

    fun findById(id: Long): Optional<Vehicle> {
        return vehicleRepository.findById(id)
    }

    fun existsById(id: Long): Boolean {
        return vehicleRepository.existsById(id)
    }

    fun deleteById(id: Long) {
        vehicleRepository.deleteById(id)
    }
}