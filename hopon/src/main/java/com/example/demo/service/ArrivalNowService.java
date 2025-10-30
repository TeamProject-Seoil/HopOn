// src/main/java/com/example/demo/service/ArrivalNowService.java
package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.repository.StopRepository;
// ⚠ StopRow import는 더 이상 안 써도 됩니다. (충돌 방지 차원에서 제거 권장)
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ArrivalNowService {

    private final StationStopListService stationStopListService;
    private final StopRepository stopRepository;

    @Autowired(required = false)
    private BusRouteInfoService routeInfoService;

    @Autowired(required = false)
    private BusStopListService busStopListService;

    public ArrivalNowResponse build(String routeId, BusLocationDto bus, String fallbackPlainNo) {
        Integer routeTypeCode  = resolveRouteTypeCode(routeId);
        String  routeTypeLabel = toRouteTypeLabel(routeTypeCode);

        Integer currSeq = parseSectOrd(bus);
        if (currSeq == null && bus != null && bus.getGpsY() != null && bus.getGpsX() != null) {
            currSeq = stopRepository.findNearestSeq(routeId, bus.getGpsY(), bus.getGpsX());
        }
        if (currSeq == null) {
            return empty(routeTypeCode, routeTypeLabel);
        }

        Integer maxSeq = stopRepository.findMaxSeq(routeId);
        if (maxSeq == null || maxSeq < 1) {
            return empty(routeTypeCode, routeTypeLabel);
        }
        boolean circular = stopRepository.isCircularRoute(routeId);
        int nextSeq = (currSeq >= maxSeq) ? (circular ? 1 : maxSeq) : (currSeq + 1);

        // ▼▼▼ 여기부터 RAW 사용 ▼▼▼
        StopRowRaw curr = toStopRowRaw(stopRepository.findStopRowBySeqRaw(routeId, currSeq));
        StopRowRaw next = toStopRowRaw(stopRepository.findStopRowBySeqRaw(routeId, nextSeq));

        String currentName = (curr != null && !isBlank(curr.stopName)) ? curr.stopName : "-";
        String nextName    = (next != null && !isBlank(next.stopName)) ? next.stopName : "-";

        Integer etaSec = null;
        if (curr != null && !isBlank(curr.arsId)) {
            List<StationStopListDto> arrivals = stationStopListService.getStationByUid(curr.arsId);
            if (arrivals != null && !arrivals.isEmpty()) {
                for (StationStopListDto row : arrivals) {
                    if (!routeId.equals(row.getBusRouteId())) continue;
                    Integer e1 = ArrmsgParser.toSeconds(row.getArrmsg1());
                    Integer e2 = ArrmsgParser.toSeconds(row.getArrmsg2());
                    Integer cand = minIgnoreNull(e1, e2);
                    if (cand != null) etaSec = (etaSec == null) ? cand : Math.min(etaSec, cand);
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
    private Integer parseSectOrd(BusLocationDto bus) {
        try {
            String so = (bus != null) ? bus.getSectOrd() : null;
            if (!StringUtils.hasText(so)) return null;
            int seq = Integer.parseInt(so.trim());
            return (seq >= 1) ? seq : null;
        } catch (Exception e) { return null; }
    }

    private boolean isBlank(String s) { return s == null || s.isBlank(); }
    private Integer minIgnoreNull(Integer a, Integer b) { if (a == null) return b; if (b == null) return a; return Math.min(a, b); }

    private Integer resolveRouteTypeCode(String routeId) {
        if (StringUtils.hasText(routeId) && routeInfoService != null) {
            try {
                RouteInfoDto info = routeInfoService.getRouteInfo(routeId);
                if (info != null && info.getBusRouteType() != null) return info.getBusRouteType();
            } catch (Exception ignore) {}
        }
        if (busStopListService != null) {
            try {
                var stops = busStopListService.getStaionByRoute(routeId);
                if (stops != null) {
                    for (var s : stops) {
                        try {
                            Integer c = Integer.parseInt(String.valueOf(s.getRouteType()).trim());
                            if (c != null) return c;
                        } catch (Exception ignore) {}
                    }
                }
            } catch (Exception ignore) {}
        }
        return null;
    }

    public String toRouteTypeLabel(Integer code) { return toRouteTypeLabel(code, null); }
    private String toRouteTypeLabel(Integer code, String fallback) {
        if (code == null) return (fallback != null ? fallback : "기타");
        return switch (code) {
            case 1 -> "공항"; case 2 -> "마을"; case 3 -> "간선"; case 4 -> "지선";
            case 5 -> "순환"; case 6 -> "광역"; case 7 -> "인천"; case 8 -> "경기";
            case 9 -> "폐지"; case 0 -> "공용"; default -> "기타";
        };
    }

    public RouteTypeMeta resolveRouteType(String routeId) {
        Integer code = resolveRouteTypeCode(routeId);
        return new RouteTypeMeta(code, toRouteTypeLabel(code));
    }

    public static class RouteTypeMeta {
        public final Integer code;
        public final String  label;
        public RouteTypeMeta(Integer code, String label) { this.code = code; this.label = label; }
    }

    // ---------- RAW 매핑용 작은 래퍼 ----------
    private static final class StopRowRaw {
        final Integer seq; final String stopName; final String arsId;
        StopRowRaw(Integer seq, String stopName, String arsId) {
            this.seq = seq; this.stopName = stopName; this.arsId = arsId;
        }
    }

    private StopRowRaw toStopRowRaw(Object raw) {
        if (raw == null) return null;
        Object[] a = (Object[]) raw;
        Integer seq      = a[0] == null ? null : ((Number)a[0]).intValue();
        String  stopName = a[1] == null ? null : a[1].toString();
        String  arsId    = a[2] == null ? null : a[2].toString();
        return new StopRowRaw(seq, stopName, arsId);
    }
}
