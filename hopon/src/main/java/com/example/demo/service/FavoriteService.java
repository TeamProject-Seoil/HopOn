
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

// FavoriteService.java
@Service
@RequiredArgsConstructor
public class FavoriteService {
    private final FavoriteRepository favoriteRepository;

    @Transactional
    public FavoriteResponse add(UserEntity user, FavoriteCreateRequest dto) {
        // 1) 입력 정규화 + 필수 검증
        String routeId = safe(dto.getRouteId());
        String direction = safe(dto.getDirection());
        String boardStopId = safe(dto.getBoardStopId());
        String destStopId  = safe(dto.getDestStopId());

        if (routeId.isEmpty() || boardStopId.isEmpty() || destStopId.isEmpty()) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "MISSING_REQUIRED_FIELDS");
        }

        // 2) 노선유형 보정(둘 중 하나만 와도 채워줌)
        Integer typeCode = dto.getBusRouteType();
        String  typeName = trimNull(dto.getRouteTypeName());
     // 2-1) 숫자 문자열이 오면 코드로 인식
        if (typeCode == null && typeName != null && typeName.matches("\\d+")) {
            typeCode = Integer.parseInt(typeName);
        }

        // 2-2) 한쪽만 있으면 다른 쪽 채우기
        if (typeCode == null && typeName != null) {
            typeCode = toCode(typeName);           // "지선" → 4, "4" → 4
        }
        if (typeCode != null && (typeName == null || typeName.isBlank() || typeName.matches("\\d+"))) {
            typeName = toLabel(typeCode);          // **항상 라벨 보장: 4 → "지선"**
        }
        // 3) 중복 체크(정규화된 값으로)
        boolean dup = favoriteRepository
                .existsByUserAndRouteIdAndDirectionAndBoardStopIdAndDestStopId(
                        user, routeId, direction, boardStopId, destStopId
                );
        if (dup) {
            // a) 지금처럼 409 유지
            throw new ResponseStatusException(CONFLICT, "DUPLICATE_FAVORITE");

            // b) (선택) 멱등 처리 원하면 아래로 대체:
            // FavoriteEntity existed = favoriteRepository
            //     .findFirstByUserAndRouteIdAndDirectionAndBoardStopIdAndDestStopId(
            //           user, routeId, direction, boardStopId, destStopId)
            //     .orElseThrow(); // 존재 확정
            // return toRes(existed);
        }

        FavoriteEntity saved = favoriteRepository.save(
            FavoriteEntity.builder()
                    .user(user)
                    .routeId(routeId)
                    .direction(direction)
                    .boardStopId(boardStopId)
                    .boardStopName(trimNull(dto.getBoardStopName()))
                    .boardArsId(trimNull(dto.getBoardArsId()))
                    .destStopId(destStopId)
                    .destStopName(trimNull(dto.getDestStopName()))
                    .destArsId(trimNull(dto.getDestArsId()))
                    .routeName(nonEmptyOr(dto.getRouteName(), "")) // 널 방지
                    .busRouteType(typeCode)
                    .routeTypeName(typeName)
                    .build()
        );
        return toRes(saved);
    }

    @Transactional(readOnly = true)
    public List<FavoriteResponse> listAll(UserEntity user) {
        return favoriteRepository.findByUserOrderByUpdatedAtDesc(user)
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
                .busRouteType(e.getBusRouteType())
                .routeTypeName(e.getRouteTypeName())
                .build();
    }

    // ===== 헬퍼 =====
    private static String trimNull(String s) { return s == null ? null : s.trim(); }
    private static String safe(String s) { return s == null ? "" : s.trim(); }
    private static String nonEmptyOr(String s, String fallback) {
        String t = trimNull(s);
        return (t == null || t.isEmpty()) ? fallback : t;
    }

    // 코드↔라벨 매핑(클라가 둘 중 하나만 보내도 서버가 맞춰 저장)
    private static int toCode(String label) {
        if (label == null) return 0;
        String s = label.trim();
        // 숫자면 바로 코드로 인정
        if (s.matches("\\d+")) return Integer.parseInt(s);
        return switch (s) {
            case "공항" -> 1; case "마을" -> 2; case "간선" -> 3; case "지선" -> 4;
            case "순환" -> 5; case "광역" -> 6; case "인천" -> 7; case "경기" -> 8;
            case "폐지" -> 9; default -> 0;
        };
    }
    private static String toLabel(Integer code) {
        if (code == null) return null;
        return switch (code) {
            case 1 -> "공항"; case 2 -> "마을"; case 3 -> "간선"; case 4 -> "지선";
            case 5 -> "순환"; case 6 -> "광역"; case 7 -> "인천"; case 8 -> "경기";
            case 9 -> "폐지"; default -> "공용";
        };
    }
}