package com.example.demo.repository;

import com.example.demo.entity.BusVehicleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BusVehicleRepository extends JpaRepository<BusVehicleEntity, String> {
    Optional<BusVehicleEntity> findByPlateNo(String plateNo);
}
