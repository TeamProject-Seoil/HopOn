
package com.example.demo.service;

import com.example.demo.dto.FavoriteCreateRequest;
import com.example.demo.dto.FavoriteResponse;
import com.example.demo.entity.FavoriteEntity;
import com.example.demo.entity.UserEntity;
import com.example.demo.repository.FavoriteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.*;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;

    @Transactional
    public FavoriteResponse add(UserEntity user, FavoriteCreateRequest dto) {
        // 중복 방지
        boolean dup = favoriteRepository.existsByUserAndRouteIdAndBoardStopIdAndDestStopId(
                user, dto.getRouteId(), dto.getBoardStopId(), dto.getDestStopId());
        if (dup) throw new ResponseStatusException(CONFLICT, "DUPLICATE_FAVORITE");

        FavoriteEntity saved = favoriteRepository.save(
                FavoriteEntity.builder()
                        .user(user)
                        .routeId(dto.getRouteId())
                        .direction(dto.getDirection() == null ? "" : dto.getDirection())
                        .boardStopId(dto.getBoardStopId())
                        .boardStopName(dto.getBoardStopName())
                        .boardArsId(dto.getBoardArsId())
                        .destStopId(dto.getDestStopId())
                        .destStopName(dto.getDestStopName())
                        .destArsId(dto.getDestArsId())
                        .routeName(dto.getRouteName())
                        .build()
        );
        return toRes(saved);
    }

    @Transactional(readOnly = true)
    public List<FavoriteResponse> top3(UserEntity user) {
        return favoriteRepository.findTop3ByUserOrderByUpdatedAtDesc(user)
                .stream().map(this::toRes).toList();
    }

    @Transactional
    public void remove(UserEntity user, Long id) {
        FavoriteEntity found = favoriteRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "FAVORITE_NOT_FOUND"));
        favoriteRepository.delete(found);
    }

    private FavoriteResponse toRes(FavoriteEntity e) {
        return FavoriteResponse.builder()
                .id(e.getId())
                .routeId(e.getRouteId())
                .direction(e.getDirection())
                .boardStopId(e.getBoardStopId())
                .boardStopName(e.getBoardStopName())
                .boardArsId(e.getBoardArsId())
                .destStopId(e.getDestStopId())
                .destStopName(e.getDestStopName())
                .destArsId(e.getDestArsId())
                .routeName(e.getRouteName())
                .build();
    }
}
