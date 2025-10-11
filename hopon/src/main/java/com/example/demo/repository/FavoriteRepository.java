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

    List<FavoriteEntity> findTop3ByUserOrderByUpdatedAtDesc(UserEntity user);

    Optional<FavoriteEntity> findByIdAndUser(Long id, UserEntity user);

    boolean existsByUserAndRouteIdAndBoardStopIdAndDestStopId(
            UserEntity user, String routeId, String boardStopId, String destStopId);
}
