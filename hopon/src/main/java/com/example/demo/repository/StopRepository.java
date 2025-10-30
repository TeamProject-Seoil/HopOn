// src/main/java/com/example/demo/repository/StopRepository.java
package com.example.demo.repository;

import com.example.demo.entity.Stop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StopRepository extends JpaRepository<Stop, String> {

    @Query(value = """
        SELECT lon AS lon, lat AS lat
        FROM route_stop_seq
        WHERE route_id = :routeId AND ars_id = :arsId
        LIMIT 1
        """, nativeQuery = true)
    StopCoord findCoord(@Param("routeId") String routeId,
                        @Param("arsId") String arsId);

    @Query(value = """
        SELECT seq
        FROM route_stop_seq
        WHERE route_id = :routeId AND ars_id = :arsId
        LIMIT 1
        """, nativeQuery = true)
    Integer findSeq(@Param("routeId") String routeId,
                    @Param("arsId") String arsId);

    // ▼ 추가: 특정 seq의 정류장 이름/ARS
    @Query(value = """
        SELECT seq AS seq, stop_name AS stopName, ars_id AS arsId
        FROM route_stop_seq
        WHERE route_id = :routeId AND seq = :seq
        LIMIT 1
        """, nativeQuery = true)
    StopRow findStopRowBySeq(@Param("routeId") String routeId,
                             @Param("seq") int seq);

    // ▼ 추가: 해당 노선의 최댓 seq
    @Query(value = "SELECT MAX(seq) FROM route_stop_seq WHERE route_id = :routeId", nativeQuery = true)
    Integer findMaxSeq(@Param("routeId") String routeId);

    // ▼ 추가: 좌표 기준 최근접 seq (MySQL 8, SRID 4326 좌표)
    @Query(value = """
        SELECT seq
        FROM route_stop_seq
        WHERE route_id = :routeId
        ORDER BY ST_Distance_Sphere(POINT(lon, lat), POINT(:lon, :lat)) ASC
        LIMIT 1
        """, nativeQuery = true)
    Integer findNearestSeq(@Param("routeId") String routeId,
                           @Param("lat") double lat,
                           @Param("lon") double lon);

    @Query(value = """
            SELECT seq, stop_name, ars_id
            FROM route_stop_seq
            WHERE route_id = :routeId AND seq = :seq
            LIMIT 1
            """, nativeQuery = true)
        Object findStopRowBySeqRaw(@Param("routeId") String routeId,
                                   @Param("seq") int seq);
    
    // 기본 구현: 순환노선 여부(필요 시 프로젝트 정책에 맞게 오버라이드)
    default boolean isCircularRoute(String routeId) { return false; }
}
