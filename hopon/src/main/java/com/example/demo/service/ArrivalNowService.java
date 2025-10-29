// src/main/java/com/example/demo/service/ArrivalNowService.java
package com.example.demo.service;

import com.example.demo.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ArrivalNowService {

    private final BusStopListService busStopListService;
    private final StationStopListService stationStopListService;

    @Autowired(required = false)
    private BusRouteInfoService routeInfoService; // 없으면 null

    /**
     * - 노선 정류장 목록(getStaionByRoute)에서 현재/다음 정류장 산출
     * - 현재 정류장 도착정보(getStationByUid)로 ETA 근사
     * - 노선유형: 1순위 routeInfoService, 2순위 정류장 목록의 routeType, 3순위 null
     */
    public ArrivalNowResponse build(String routeId, BusLocationDto bus, String fallbackPlainNo) {
        // 1) 노선의 정류장 목록
        List<BusStopListDto> stops = busStopListService.getStaionByRoute(routeId);

        // 1-1) 노선유형 메타
        Integer routeTypeCode  = resolveRouteTypeCode(routeId, stops);
        String  routeTypeLabel = toRouteTypeLabel(routeTypeCode);

        if (stops == null || stops.isEmpty()) {
            // 정류장 데이터가 없어도 메타는 유지
            return empty(routeTypeCode, routeTypeLabel);
        }

        // 2) sectOrd 기반 seq, 없으면 좌표 근접
        Integer currSeq = seqFromSectOrd(bus, stops);
        if (currSeq == null) currSeq = nearestSeqByCoord(bus, stops);

        // 3) 현재/다음 정류장
        BusStopListDto current = findBySeq(stops, currSeq);
        BusStopListDto next    = findBySeq(stops, currSeq == null ? null : currSeq + 1);
        String currentName = current != null ? nz(current.getStationNm(), "-") : "-";
        String nextName    = next    != null ? nz(next.getStationNm(), "-")    : "-";

        // 4) ETA 근사
        Integer etaSec = null;
        String arsForEta = current != null ? current.getArsId() : null;
        if (StringUtils.hasText(arsForEta)) {
            List<StationStopListDto> arrivals = stationStopListService.getStationByUid(arsForEta);
            if (arrivals != null && !arrivals.isEmpty()) {
                for (StationStopListDto row : arrivals) {
                    if (!routeId.equals(row.getBusRouteId())) continue;
                    Integer e1 = ArrmsgParser.toSeconds(row.getArrmsg1());
                    Integer e2 = ArrmsgParser.toSeconds(row.getArrmsg2());
                    Integer candidate = minIgnoreNull(e1, e2);
                    if (candidate != null) etaSec = (etaSec == null) ? candidate : Math.min(etaSec, candidate);
                }
            }
        }

        return ArrivalNowResponse.builder()
                .currentStopName(currentName)
                .nextStopName(nextName)
                .etaSec(etaSec)
                .routeTypeCode(routeTypeCode)
                .routeTypeLabel(routeTypeLabel)
                .build();
    }

    /** 매칭/데이터 없을 때 기본 응답(메타 유지) — 다른 서비스에서도 쓸 수 있게 public */
    public ArrivalNowResponse empty(Integer routeTypeCode, String routeTypeLabel) {
        return ArrivalNowResponse.builder()
                .currentStopName("-")
                .nextStopName("-")
                .etaSec(null)
                .routeTypeCode(routeTypeCode)
                .routeTypeLabel(toRouteTypeLabel(routeTypeCode != null ? routeTypeCode : null, routeTypeLabel))
                .build();
    }

    // ---------- helpers ----------
    private Integer seqFromSectOrd(BusLocationDto bus, List<BusStopListDto> stops) {
        try {
            String so = bus != null ? bus.getSectOrd() : null;
            if (!StringUtils.hasText(so)) return null;
            int seq = Integer.parseInt(so);
            int maxSeq = stops.stream().map(BusStopListDto::getSeq).max(Comparator.naturalOrder()).orElse(0);
            if (seq < 1 || seq > maxSeq) return null;
            return seq;
        } catch (Exception e) { return null; }
    }

    private Integer nearestSeqByCoord(BusLocationDto bus, List<BusStopListDto> stops) {
        if (bus == null || bus.getGpsY() == null || bus.getGpsX() == null) return null;
        double lat = bus.getGpsY(), lon = bus.getGpsX();
        double best = Double.MAX_VALUE; Integer bestSeq = null;
        for (BusStopListDto s : stops) {
            if (s.getGpsY() == null || s.getGpsX() == null) continue;
            double d2 = dist2(lat, lon, s.getGpsY(), s.getGpsX());
            if (d2 < best) { best = d2; bestSeq = s.getSeq(); }
        }
        return bestSeq;
    }

    private double dist2(double lat1, double lon1, double lat2, double lon2) {
        double dy = lat1 - lat2, dx = lon1 - lon2; return dx*dx + dy*dy;
    }

    private BusStopListDto findBySeq(List<BusStopListDto> stops, Integer seq) {
        if (seq == null) return null;
        for (BusStopListDto s : stops) if (Objects.equals(s.getSeq(), seq)) return s;
        return null;
    }

    private Integer minIgnoreNull(Integer a, Integer b) {
        if (a == null) return b; if (b == null) return a; return Math.min(a, b);
    }
    private String nz(String s, String d) { return (s == null || s.isBlank()) ? d : s; }

    // --- 노선유형 코드/라벨 ---
    public Integer resolveRouteTypeCode(String routeId, List<BusStopListDto> stops) {
        if (StringUtils.hasText(routeId) && routeInfoService != null) {
            try {
                RouteInfoDto info = routeInfoService.getRouteInfo(routeId);
                if (info != null && info.getBusRouteType() != null) return info.getBusRouteType();
            } catch (Exception ignore) {}
        }
        if (stops != null) {
            for (BusStopListDto s : stops) {
                Integer c = safeParseInt(s.getRouteType());
                if (c != null) return c;
            }
        }
        return null;
    }

    /** 라벨 변환은 다른 서비스에서도 쓰므로 public */
    public String toRouteTypeLabel(Integer code) { return toRouteTypeLabel(code, null); }

    private String toRouteTypeLabel(Integer code, String fallback) {
        if (code == null) return (fallback != null ? fallback : "기타");
        return switch (code) {
            case 1 -> "공항"; case 2 -> "마을"; case 3 -> "간선"; case 4 -> "지선";
            case 5 -> "순환"; case 6 -> "광역"; case 7 -> "인천"; case 8 -> "경기";
            case 9 -> "폐지"; case 0 -> "공용"; default -> "기타";
        };
    }

    private Integer safeParseInt(Object o) { try { return o==null?null:Integer.parseInt(String.valueOf(o).trim()); } catch (Exception e) { return null; } }
}
