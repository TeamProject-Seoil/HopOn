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

    // (참고) 이건 3컬럼이지만, ArrivalNowService에서는 Raw(Object[]) 버전만 사용하니 그대로 둬도 무방
    @Query(value = """
        SELECT seq AS seq, stop_name AS stopName, ars_id AS arsId
        FROM route_stop_seq
        WHERE route_id = :routeId AND seq = :seq
        LIMIT 1
        """, nativeQuery = true)
    StopRow findStopRowBySeq(@Param("routeId") String routeId,
                             @Param("seq") int seq);

    @Query(value = "SELECT MAX(seq) FROM route_stop_seq WHERE route_id = :routeId", nativeQuery = true)
    Integer findMaxSeq(@Param("routeId") String routeId);

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

    // ★★★★★ 여기! 4컬럼 반환으로 수정 (seq, stop_name, ars_id, st_id)
    @Query(value = """
        SELECT rss.seq, rss.stop_name, rss.ars_id, rss.st_id
        FROM route_stop_seq rss
        WHERE rss.route_id = :routeId AND rss.seq = :seq
        LIMIT 1
        """, nativeQuery = true)
    Object findStopRowBySeqRaw(@Param("routeId") String routeId,
                               @Param("seq") int seq);

    default boolean isCircularRoute(String routeId) { return false; }

    @Query(value = """
        SELECT rss.st_id
        FROM route_stop_seq rss
        WHERE rss.route_id = :routeId AND rss.seq = :seq
        LIMIT 1
        """, nativeQuery = true)
    String findStIdByRouteAndSeq(@Param("routeId") String routeId,
                                 @Param("seq") int seq);
}
