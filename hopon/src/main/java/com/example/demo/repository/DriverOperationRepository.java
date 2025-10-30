package com.example.demo.repository;

import com.example.demo.entity.DriverOperation;
import com.example.demo.entity.DriverOperationStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DriverOperationRepository extends JpaRepository<DriverOperation, Long> {
    Optional<DriverOperation> findFirstByUserNumAndStatus(Long userNum, DriverOperationStatus status);
    List<DriverOperation> findByUserNumOrderByStartedAtDesc(Long userNum);

    // ⬇️ 추가
    List<DriverOperation> findByRouteIdAndStatusOrderByUpdatedAtDesc(String routeId, DriverOperationStatus status);


    // 페이징 목록
    Page<DriverOperation> findByUserNum(Long userNum, Pageable pageable);
    Page<DriverOperation> findByUserNumAndStatus(Long userNum, DriverOperationStatus status, Pageable pageable);
    // 종료 단건 조회 (소유자 검증)
    Optional<DriverOperation> findByIdAndUserNumAndStatus(Long id, Long userNum, DriverOperationStatus status);
    List<DriverOperation> findByUserNumAndStatusOrderByEndedAtDesc(Long userNum, DriverOperationStatus status);
    Optional<DriverOperation> findByIdAndUserNum(Long id, Long userNum);
    

Optional<DriverOperation> findFirstByApiVehIdAndStatusOrderByUpdatedAtDesc(
        String apiVehId, DriverOperationStatus status);

Optional<DriverOperation> findFirstByApiPlainNoAndStatusOrderByUpdatedAtDesc(
        String apiPlainNo, DriverOperationStatus status);
}
