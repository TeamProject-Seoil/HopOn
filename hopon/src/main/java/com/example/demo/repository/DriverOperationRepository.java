package com.example.demo.repository;

import com.example.demo.entity.DriverOperation;
import com.example.demo.entity.DriverOperationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DriverOperationRepository extends JpaRepository<DriverOperation, Long> {
    Optional<DriverOperation> findFirstByUserNumAndStatus(Long userNum, DriverOperationStatus status);
    List<DriverOperation> findByUserNumOrderByStartedAtDesc(Long userNum);

    // ⬇️ 추가
    List<DriverOperation> findByRouteIdAndStatusOrderByUpdatedAtDesc(String routeId, DriverOperationStatus status);
}
