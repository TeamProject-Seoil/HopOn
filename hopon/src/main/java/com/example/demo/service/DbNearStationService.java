package com.example.demo.service;

import com.example.demo.dto.DbNearStationDto;
import com.example.demo.repository.DbNearStationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class DbNearStationService {

    private final DbNearStationRepository dbNearStationRepository;

    public DbNearStationService(DbNearStationRepository dbNearStationRepository) {
        this.dbNearStationRepository = dbNearStationRepository;
    }

    /**
     * 특정 좌표 주변의 버스 정류장을 검색합니다.
     * @param latitude 위도 (Y)
     * @param longitude 경도 (X)
     * @param radius 검색 반경 (미터)
     * @return 주변 정류장 리스트
     */
    public List<DbNearStationDto> findNearbyStations(double latitude, double longitude, int radius) {
        return dbNearStationRepository.findNearbyStops(latitude, longitude, radius);
    }
}
