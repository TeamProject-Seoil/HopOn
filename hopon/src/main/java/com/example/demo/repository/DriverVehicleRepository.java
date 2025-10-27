package com.example.demo.repository;

import com.example.demo.entity.DriverVehicle;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.Optional;

public interface DriverVehicleRepository extends JpaRepository<DriverVehicle, Long> {
    Optional<DriverVehicle> findByUserNum(Long userNum);
    
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    int deleteByUserNum(Long userNum);
}
