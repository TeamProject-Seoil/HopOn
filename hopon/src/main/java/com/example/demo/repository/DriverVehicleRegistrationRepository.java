// src/main/java/com/example/demo/repository/DriverVehicleRegistrationRepository.java
package com.example.demo.repository;

import com.example.demo.entity.DriverVehicleRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DriverVehicleRegistrationRepository extends JpaRepository<DriverVehicleRegistration, Long> {
    boolean existsByUserNumAndVehicleId(Long userNum, String vehicleId);
    Optional<DriverVehicleRegistration> findByUserNumAndVehicleId(Long userNum, String vehicleId);
    List<DriverVehicleRegistration> findByUserNumOrderByCreatedAtDesc(Long userNum);
    void deleteByUserNumAndVehicleId(Long userNum, String vehicleId);
}
