package com.example.demo.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.example.demo.repository.StopCoord;
import com.example.demo.repository.StopRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.demo.dto.BusRoutePathDto;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class BusRoutePathService {
    private final WebClient webClient;
    private final StopRepository stopRepository;

    @Value("${publicdata.serviceKey}")
    private String serviceKey;

    /** 1) 전체 노선 polyline: getRoutePathList (JSON) */
    public List<BusRoutePathDto> getRoutePathList(String busRouteId) {
        JsonNode root = webClient.get()
                .uri(uri -> uri
                        .path("/busRouteInfo/getRoutePath")
                        .queryParam("serviceKey", serviceKey)
                        .queryParam("busRouteId", busRouteId)
                        .queryParam("resultType", "json")   // ✅ JSON으로 받기
                        .build())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (root == null) return List.of();

        // 응답 루트는 환경에 따라 다르게 내려올 수 있어 둘 다 대응
        JsonNode items = root.path("msgBody").path("itemList");
        if (!items.isArray()) {
            items = root.path("response").path("msgBody").path("itemList");
        }
        if (!items.isArray()) return List.of();

        // no 기준 정렬
        List<Temp> buf = new ArrayList<>();
        for (JsonNode n : items) {
            int no = n.path("no").asInt();
            double lon = n.path("gpsX").asDouble(); // 경도
            double lat = n.path("gpsY").asDouble(); // 위도
            buf.add(new Temp(no, lat, lon));
        }
        buf.sort(Comparator.comparingInt(t -> t.no));

        List<BusRoutePathDto> path = new ArrayList<>(buf.size());
        for (Temp t : buf) path.add(new BusRoutePathDto(t.lat, t.lng));
        return path;
    }

    /** 2) 승차~하차 구간 슬라이스 (DB의 route_stop_seq 사용) */
    public List<BusRoutePathDto> getSegment(String routeId, String boardArsId, String destArsId) {
        List<BusRoutePathDto> path = getRoutePathList(routeId);
        if (path.isEmpty()) return List.of();

        StopCoord board = stopRepository.findCoord(routeId, boardArsId);
        StopCoord dest  = stopRepository.findCoord(routeId, destArsId);

        if (board == null || dest == null)
            throw new IllegalArgumentException("정류장 정보가 없습니다.");

        int startIdx = nearestIndex(path, board.getLat(), board.getLon());
        int endIdx   = nearestIndex(path, dest.getLat(), dest.getLon());

        List<BusRoutePathDto> segment;
        if (startIdx <= endIdx) {
            segment = new ArrayList<>(path.subList(startIdx, endIdx + 1));
        } else {
            // 순환 노선일 경우
            segment = new ArrayList<>(path.size());
            segment.addAll(path.subList(startIdx, path.size()));
            segment.addAll(path.subList(0, endIdx + 1));
        }

        // ✅ 여기서 보간 메서드 호출
        return attachStopsToEnds(segment,
                board.getLat(), board.getLon(),
                dest.getLat(), dest.getLon(),
                120   // 최대 연결 거리 (m)
        );
    }
    // --- 추가 메서드 ---
    private List<BusRoutePathDto> attachStopsToEnds(
            List<BusRoutePathDto> segment,
            double boardLat, double boardLng,
            double destLat, double destLng,
            double maxSnapMeters
    ) {
        if (segment.isEmpty()) return segment;

        var first = segment.get(0);
        var last  = segment.get(segment.size()-1);

        if (haversineM(boardLat, boardLng, first.getLat(), first.getLng()) <= maxSnapMeters) {
            segment.add(0, new BusRoutePathDto(boardLat, boardLng));
        }
        if (haversineM(destLat, destLng, last.getLat(), last.getLng()) <= maxSnapMeters) {
            segment.add(new BusRoutePathDto(destLat, destLng));
        }
        return segment;
    }

    private double haversineM(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371000.0;
        double dLat = Math.toRadians(lat2-lat1);
        double dLon = Math.toRadians(lon2-lon1);
        double a = Math.sin(dLat/2)*Math.sin(dLat/2)
                + Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon/2)*Math.sin(dLon/2);
        return 2*R*Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    }

    private int nearestIndex(List<BusRoutePathDto> path, double lat, double lng) {
        int best = 0; double bestD2 = Double.MAX_VALUE;
        for (int i = 0; i < path.size(); i++) {
            double dy = path.get(i).getLat() - lat;
            double dx = path.get(i).getLng() - lng;
            double d2 = dx*dx + dy*dy;
            if (d2 < bestD2) { bestD2 = d2; best = i; }
        }
        return best;
    }

    private record Temp(int no, double lat, double lng) {}
}
