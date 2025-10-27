package com.example.demo.repository;

import com.example.demo.entity.Stop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
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
}