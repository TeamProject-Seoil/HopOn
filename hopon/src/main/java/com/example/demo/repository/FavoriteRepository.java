// FavoriteRepository.java
package com.example.demo.repository;

import com.example.demo.entity.FavoriteEntity;
import com.example.demo.entity.UserEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<FavoriteEntity, Long> {


    // 🔥 개수 제한 로직에서 쓰던 메서드 삭제 가능: long countByUser(UserEntity user);

    boolean existsByUserAndRouteIdAndBoardStopIdAndDestStopId(
            UserEntity user, String routeId, String boardStopId, String destStopId);

    // 🔁 전체 다 조회 (최신순)
    List<FavoriteEntity> findByUserOrderByUpdatedAtDesc(UserEntity user);

    Optional<FavoriteEntity> findByIdAndUser(Long id, UserEntity user);
}
