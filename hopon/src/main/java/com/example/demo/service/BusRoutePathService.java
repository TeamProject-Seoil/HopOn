package com.example.demo.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.example.demo.repository.StopCoord;
import com.example.demo.repository.StopRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
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
    @Cacheable(value = "routePolylineCache", key = "#busRouteId", unless="#result == null || #result.isEmpty()")
    public List<BusRoutePathDto> getRoutePathList(String busRouteId) {
        JsonNode root = webClient.get()
                .uri(uri -> uri
                        .path("/busRouteInfo/getRoutePath")
                        .queryParam("serviceKey", serviceKey)
                        .queryParam("busRouteId", busRouteId)
                        .queryParam("resultType", "json")
                        .build())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (root == null) throw new IllegalStateException("getRoutePath null response");

        JsonNode items = root.path("msgBody").path("itemList");
        if (!items.isArray()) {
            items = root.path("response").path("msgBody").path("itemList");
        }
        if (!items.isArray()) throw new IllegalStateException("getRoutePath invalid payload");

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

    /** 2) 승차~하차 구간 슬라이스 (정류장 SEQ + 앞으로만 검색 + 투영 + 거리 제한 스냅) */
    public List<BusRoutePathDto> getSegment(String routeId, String boardArsId, String destArsId) {
        // 전체 폴리라인 (API 또는 캐시)
        List<BusRoutePathDto> path = getRoutePathList(routeId);
        if (path.isEmpty()) return List.of();

        // DB에서 정류장 좌표 + 순서 가져오기
        StopCoord board = stopRepository.findCoord(routeId, boardArsId);
        StopCoord dest  = stopRepository.findCoord(routeId, destArsId);
        Integer boardSeq = stopRepository.findSeq(routeId, boardArsId);
        Integer destSeq  = stopRepository.findSeq(routeId, destArsId);

        if (board == null || dest == null || boardSeq == null || destSeq == null) {
            throw new IllegalArgumentException("정류장 정보(좌표/순서)를 찾을 수 없습니다.");
        }

        // 순서 역전 시(같은 방면 강제): 필요하면 순환 노선 정책에 따라 허용/교체
        boolean circular = stopRepository.isCircularRoute(routeId); // 없으면 false 리턴하게 구현
        if (boardSeq > destSeq && !circular) {
            // 같은 방면만 허용한다면 여기서 예외 처리
            throw new IllegalArgumentException("진행 방향이 맞지 않습니다. (순환 노선이 아님)");
        }

        // 1) 시작점 인덱스: 전체에서 최근접
        int startIdx = nearestIndexDeg(path, board.getLat(), board.getLon());

        // 2) 끝점 인덱스: 시작 인덱스 이후 "앞으로만" 탐색해서 최근접 (되돌림 방지)
        int endIdx = forwardNearestIndex(path, dest.getLat(), dest.getLon(), startIdx, /*검색폭*/ 2000);

        // 순환 노선 처리: endIdx가 startIdx보다 앞이면 wrap-around
        List<BusRoutePathDto> segment;
        if (startIdx <= endIdx) {
            segment = new ArrayList<>(path.subList(startIdx, endIdx + 1));
        } else {
            if (!circular) {
                // 비순환인데 이렇게 나오면 안전장치로 swap 처리 (또는 예외)
                int tmp = startIdx; startIdx = endIdx; endIdx = tmp;
                segment = new ArrayList<>(path.subList(startIdx, endIdx + 1));
            } else {
                segment = new ArrayList<>(path.size());
                segment.addAll(path.subList(startIdx, path.size()));
                segment.addAll(path.subList(0, endIdx + 1));
            }
        }

        // 3) 폴리라인 위에 ‘투영’해서 정확한 시작/끝 점으로 보정
        //    (정류장 좌표를 가장 가까운 세그먼트에 수직 투영해서 점 삽입)
        segment = projectEndsOnPolyline(segment, board.getLat(), board.getLon(), dest.getLat(), dest.getLon());

        // 4) 과도한 스냅 방지: 정류장과 첫/끝 점이 너무 멀면(예: 120m 초과) 스냅하지 않음
        segment = attachStopsToEnds(segment, board.getLat(), board.getLon(), dest.getLat(), dest.getLon(), 120);

        return segment;
    }

    // ====== 보조 메서드들 ======

    /** 시작 인덱스 이후(앞으로만)에서 target에 가장 가까운 인덱스 찾기 */
    private int forwardNearestIndex(List<BusRoutePathDto> path, double lat, double lng,
                                    int startIdx, int searchWindow) {
        int n = path.size();
        int end = Math.min(n - 1, startIdx + Math.max(10, searchWindow)); // 과한 탐색 방지
        int best = startIdx;
        double bestD2 = Double.MAX_VALUE;

        for (int i = startIdx; i <= end; i++) {
            double dy = path.get(i).getLat() - lat;
            double dx = path.get(i).getLng() - lng;
            double d2 = dx * dx + dy * dy;
            if (d2 < bestD2) {
                bestD2 = d2; best = i;
            }
        }
        return best;
    }

    /** 전체 경로에서 최근접 인덱스(도/경도 제곱거리) */
    private int nearestIndexDeg(List<BusRoutePathDto> path, double lat, double lng) {
        int best = 0; double bestD2 = Double.MAX_VALUE;
        for (int i = 0; i < path.size(); i++) {
            double dy = path.get(i).getLat() - lat;
            double dx = path.get(i).getLng() - lng;
            double d2 = dx * dx + dy * dy;
            if (d2 < bestD2) { bestD2 = d2; best = i; }
        }
        return best;
    }

    /** 끝점 두 개를 각각 폴리라인에 ‘투영’하여 선 위의 점으로 교체/삽입 */
    private List<BusRoutePathDto> projectEndsOnPolyline(List<BusRoutePathDto> segment,
                                                        double boardLat, double boardLng,
                                                        double destLat, double destLng) {
        if (segment.size() < 2) return segment;

        // 시작 투영
        ProjectionResult pStart = projectPointToPolyline(segment, boardLat, boardLng);
        if (pStart != null) {
            // 세그먼트 내부에 투영되면 그 위치에 점 삽입/교체
            segment = insertProjectedPoint(segment, pStart);
        }

        // 끝 투영 (마지막 세그먼트 기준이 아니라 전체 segment 기준으로 다시 계산)
        ProjectionResult pEnd = projectPointToPolyline(segment, destLat, destLng);
        if (pEnd != null) {
            segment = insertProjectedPoint(segment, pEnd);
        }
        return segment;
    }

    /** 특정 점을 폴리라인(연속 선분) 위로 수직 투영 */
    private ProjectionResult projectPointToPolyline(List<BusRoutePathDto> poly, double lat, double lng) {
        int bestSegIdx = -1;
        double bestDist2 = Double.MAX_VALUE;
        double projLat = 0, projLng = 0;

        for (int i = 0; i < poly.size() - 1; i++) {
            double ax = poly.get(i).getLng();
            double ay = poly.get(i).getLat();
            double bx = poly.get(i + 1).getLng();
            double by = poly.get(i + 1).getLat();

            double[] proj = projectPointOnSegment(ax, ay, bx, by, lng, lat); // (x=lng, y=lat)
            double dx = proj[0] - lng;
            double dy = proj[1] - lat;
            double d2 = dx * dx + dy * dy;
            if (d2 < bestDist2) {
                bestDist2 = d2;
                bestSegIdx = i;
                projLng = proj[0];
                projLat = proj[1];
            }
        }
        return (bestSegIdx >= 0) ? new ProjectionResult(bestSegIdx, projLat, projLng) : null;
    }

    /** 점을 선분 AB 위에 투영 (좌표계: x=lng, y=lat). 결과가 세그먼트 밖이면 끝점으로 클램프 */
    private double[] projectPointOnSegment(double ax, double ay, double bx, double by, double px, double py) {
        double vx = bx - ax, vy = by - ay;
        double wx = px - ax, wy = py - ay;
        double vv = vx * vx + vy * vy;
        if (vv == 0) return new double[]{ax, ay};
        double t = (vx * wx + vy * wy) / vv;
        if (t < 0) t = 0; else if (t > 1) t = 1;
        return new double[]{ax + t * vx, ay + t * vy};
    }

    /** 투영 지점 삽입: 세그먼트 i-(i+1) 사이에 점을 끼워 넣음(중복 좌표면 스킵) */
    private List<BusRoutePathDto> insertProjectedPoint(List<BusRoutePathDto> poly, ProjectionResult pr) {
        // 이미 동일 좌표가 있으면 삽입 불필요
        BusRoutePathDto p = new BusRoutePathDto(pr.lat, pr.lng);
        if (equalsLast(poly, p) || equalsFirst(poly, p)) return poly;

        List<BusRoutePathDto> out = new ArrayList<>(poly.size() + 1);
        for (int i = 0; i < poly.size(); i++) {
            out.add(poly.get(i));
            if (i == pr.segIdx) {
                // 다음 점 들어가기 전에 투영점 삽입
                out.add(p);
            }
        }
        return out;
    }

    private boolean equalsFirst(List<BusRoutePathDto> poly, BusRoutePathDto p) {
        if (poly.isEmpty()) return false;
        return almostEqual(poly.get(0), p);
    }
    private boolean equalsLast(List<BusRoutePathDto> poly, BusRoutePathDto p) {
        if (poly.isEmpty()) return false;
        return almostEqual(poly.get(poly.size() - 1), p);
    }
    private boolean almostEqual(BusRoutePathDto a, BusRoutePathDto b) {
        // 위경도 오차 허용 (약 0.5m)
        double eps = 1e-5;
        return Math.abs(a.getLat() - b.getLat()) < eps && Math.abs(a.getLng() - b.getLng()) < eps;
    }

    /** 스냅 거리 제한 하에서만 정류장 점을 양 끝에 붙임(과도한 연결 방지) */
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
            // 첫 점이 정류장과 가깝다면 교체(삽입 대신 치환)
            segment.set(0, new BusRoutePathDto(boardLat, boardLng));
        }
        if (haversineM(destLat, destLng, last.getLat(), last.getLng()) <= maxSnapMeters) {
            segment.set(segment.size() - 1, new BusRoutePathDto(destLat, destLng));
        }
        return segment;
    }

    // ---- 유틸 ----
    private double haversineM(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371000.0;
        double dLat = Math.toRadians(lat2-lat1);
        double dLon = Math.toRadians(lon2-lon1);
        double a = Math.sin(dLat/2)*Math.sin(dLat/2)
                + Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon/2)*Math.sin(dLon/2);
        return 2*R*Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    }

    private record Temp(int no, double lat, double lng) {}

    private static class ProjectionResult {
        final int segIdx;  // 삽입할 세그먼트의 시작 인덱스 (segIdx)-(segIdx+1) 사이
        final double lat;
        final double lng;
        ProjectionResult(int segIdx, double lat, double lng) {
            this.segIdx = segIdx; this.lat = lat; this.lng = lng;
        }
    }
}