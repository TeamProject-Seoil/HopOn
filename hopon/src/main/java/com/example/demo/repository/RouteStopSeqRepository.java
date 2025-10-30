// src/main/java/com/example/demo/repository/RouteStopSeqRepository.java
package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RouteStopSeqRepository extends JpaRepository<com.example.demo.entity.Stop, String> {

    // 1) 노선의 전체 정류장 seq 목록 (정렬)
    @Query(value = """
        SELECT seq FROM route_stop_seq
        WHERE route_id = :routeId
        ORDER BY seq ASC
        """, nativeQuery = true)
    List<Integer> findSeqs(@Param("routeId") String routeId);

    // 2) 특정 seq 정류장(이름/ARS 포함)
    @Query(value = """
        SELECT seq, stop_name, ars_id
        FROM route_stop_seq
        WHERE route_id = :routeId AND seq = :seq
        LIMIT 1
        """, nativeQuery = true)
    Object findStopBySeq(@Param("routeId") String routeId, @Param("seq") int seq);

    // 3) 좌표 기준 최근접 정류장 seq (MySQL ST_Distance_Sphere)
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

    // 4) 최대 seq
    @Query(value = """
        SELECT MAX(seq) FROM route_stop_seq WHERE route_id = :routeId
        """, nativeQuery = true)
    Integer findMaxSeq(@Param("routeId") String routeId);
}
