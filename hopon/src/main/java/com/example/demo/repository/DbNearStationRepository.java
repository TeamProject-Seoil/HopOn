package com.example.demo.repository;

import com.example.demo.entity.DbNearStationEntity;
import com.example.demo.dto.DbNearStationDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DbNearStationRepository extends JpaRepository<DbNearStationEntity, String> {

    @Query(value =
            "SELECT " +
                    "    s.st_id AS stId, " +
                    "    s.ars_id AS arsId, " +
                    "    s.name AS name, " +
                    "    s.lon AS lon, " +
                    "    s.lat AS lat " + // 1. SELECT 절에서 distance 계산 부분을 제거했습니다.
                    "FROM " +
                    "    stops s " +
                    "WHERE " +
                    "    ST_Distance_Sphere(s.geom, ST_PointFromText(CONCAT('POINT(', :latitude, ' ', :longitude, ')'), 4326)) <= :radius " +
                    "ORDER BY " +
                    // 2. ORDER BY 절에서는 정렬을 위해 여전히 거리 계산 함수를 직접 사용합니다.
                    "    ST_Distance_Sphere(s.geom, ST_PointFromText(CONCAT('POINT(', :latitude, ' ', :longitude, ')'), 4326)) ASC " +
                    "LIMIT 20",
            nativeQuery = true)
    List<DbNearStationDto> findNearbyStops(
            @Param("latitude") double latitude,
            @Param("longitude") double longitude,
            @Param("radius") int radius
    );
}

