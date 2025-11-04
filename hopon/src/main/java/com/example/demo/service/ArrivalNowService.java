// src/main/java/com/example/demo/service/ArrivalNowService.java
package com.example.demo.service;

import com.example.demo.dto.ArrivalNowResponse;
import com.example.demo.dto.BusLocationDto;
import com.example.demo.dto.RouteInfoDto;
import com.example.demo.dto.StationStopListDto;
import com.example.demo.repository.StopRepository;
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

    /**
     * 현재 차량/노선에 대해 '이번/다음 정류장 + ETA + 노선유형'을 계산 후 내려준다.
     * - currentStopId/nextStopId 를 반드시 채우도록 보강
     *
     * 여기서의 정의:
     *   - currentStop = "이번 정류장" (아직 도착하지 않은, 다음에 들어갈 정류장)
     *   - nextStop    = "다음 정류장" (그 다음 정류장)
     */
    public ArrivalNowResponse build(String routeId, BusLocationDto bus, String fallbackPlainNo) {
        Integer routeTypeCode  = resolveRouteTypeCode(routeId);
        String  routeTypeLabel = toRouteTypeLabel(routeTypeCode);

        // 0) 노선의 최대 seq 미리 조회 (이후 sectOrd 보정에 사용)
        Integer maxSeq = stopRepository.findMaxSeq(routeId);
        if (maxSeq == null || maxSeq < 1) {
            return empty(routeTypeCode, routeTypeLabel);
        }

        // 1) "이번 정류장" seq 추정
        //    - sectOrd 가 "구간 번호(N번 구간 = N~N+1)" 라고 보면,
        //      사람 기준의 '이번 정류장'은 N+1 이므로 sectOrd+1 을 currSeq 로 본다.
        Integer currSeq = null;

        Integer sectOrd = parseSectOrd(bus); // 그대로 '구간 번호'만 파싱
        if (sectOrd != null) {
            int cand = sectOrd + 1;      // 구간 N → 다음 정류장 N+1 = 이번 정류장
            if (cand < 1) cand = 1;      // 하한 보정
            if (cand > maxSeq) cand = maxSeq; // 상한 보정
            currSeq = cand;
        }

        // sectOrd 로 못 얻었으면 GPS 기준으로 가장 가까운 정류장을 "이번 정류장"으로 사용
        if (currSeq == null && bus != null && bus.getGpsY() != null && bus.getGpsX() != null) {
            currSeq = stopRepository.findNearestSeq(routeId, bus.getGpsY(), bus.getGpsX());
        }
        if (currSeq == null) {
            return empty(routeTypeCode, routeTypeLabel);
        }

        // 2) "다음 정류장" seq 계산
        boolean circular = stopRepository.isCircularRoute(routeId);
        int nextSeq = (currSeq >= maxSeq) ? (circular ? 1 : maxSeq) : (currSeq + 1);

        // 3) RAW 행 조회 (seq / stop_name / ars_id / st_id 까지 뽑히도록!)
        StopRowRaw curr = toStopRowRaw(stopRepository.findStopRowBySeqRaw(routeId, currSeq)); // 이번 정류장
        StopRowRaw next = toStopRowRaw(stopRepository.findStopRowBySeqRaw(routeId, nextSeq)); // 다음 정류장

        // 4) 이름/ID 구성 (ID가 null이면 안전 fallback)
        String currentName = (curr != null && !isBlank(curr.stopName)) ? curr.stopName : "-";
        String nextName    = (next != null && !isBlank(next.stopName)) ? next.stopName : "-";

        String currentStId = (curr != null && !isBlank(curr.stId))
                ? curr.stId
                : safeFindStIdByRouteAndSeq(routeId, currSeq);

        String nextStId = (next != null && !isBlank(next.stId))
                ? next.stId
                : safeFindStIdByRouteAndSeq(routeId, nextSeq);

        // 5) ETA 추정 (ARS기반) - "이번 정류장" 기준 ETA
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
                .currentStopId(currentStId)      // = 이번 정류장 ID
                .currentStopName(currentName)    // = 이번 정류장 이름
                .nextStopId(nextStId)            // = 다음 정류장 ID
                .nextStopName(nextName)          // = 다음 정류장 이름
                .etaSec(etaSec)                  // = 이번 정류장까지 ETA(초)
                .routeTypeCode(routeTypeCode)
                .routeTypeLabel(routeTypeLabel)
                .build();
    }

    public ArrivalNowResponse empty(Integer routeTypeCode, String routeTypeLabel) {
        return ArrivalNowResponse.builder()
                .currentStopId(null)
                .currentStopName("-")
                .nextStopId(null)
                .nextStopName("-")
                .etaSec(null)
                .routeTypeCode(routeTypeCode)
                .routeTypeLabel(toRouteTypeLabel(routeTypeCode != null ? routeTypeCode : null, routeTypeLabel))
                .build();
    }

    // ---------- helpers ----------

    /**
     * sectOrd 그대로 숫자만 파싱 (구간 번호)
     * 구간 N → 정류장 seq N, N+1 사이 구간이라고 가정.
     * 실제 "이번 정류장" seq 변환은 build() 안에서 sectOrd+1 로 처리.
     */
    private Integer parseSectOrd(BusLocationDto bus) {
        try {
            String so = (bus != null) ? bus.getSectOrd() : null;
            if (!StringUtils.hasText(so)) return null;
            int seq = Integer.parseInt(so.trim());
            return (seq >= 1) ? seq : null;
        } catch (Exception e) { return null; }
    }

    private boolean isBlank(String s) { return s == null || s.isBlank(); }
    private Integer minIgnoreNull(Integer a, Integer b) {
        if (a == null) return b;
        if (b == null) return a;
        return Math.min(a, b);
    }

    public Integer resolveRouteTypeCode(String routeId) {
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

    /** 반드시 native query가 (seq, stop_name, ars_id, st_id) 순으로 반환하도록 맞춰야 함 */
    private static final class StopRowRaw {
        final Integer seq; final String stopName; final String arsId; final String stId;
        StopRowRaw(Integer seq, String stopName, String arsId, String stId) {
            this.seq = seq; this.stopName = stopName; this.arsId = arsId; this.stId = stId;
        }
    }

    private StopRowRaw toStopRowRaw(Object raw) {
        if (raw == null) return null;
        Object[] a = (Object[]) raw;
        Integer seq      = a[0] == null ? null : ((Number)a[0]).intValue();
        String  stopName = a[1] == null ? null : a[1].toString();
        String  arsId    = a[2] == null ? null : a[2].toString();
        String  stId     = a[3] == null ? null : a[3].toString();
        return new StopRowRaw(seq, stopName, arsId, stId);
    }

    /** RAW가 st_id를 못 주는 환경이면 여기 fallback 사용 */
    private String safeFindStIdByRouteAndSeq(String routeId, int seq) {
        try {
            String v = stopRepository.findStIdByRouteAndSeq(routeId, seq);
            return StringUtils.hasText(v) ? v : null;
        } catch (Exception e) {
            return null;
        }
    }

    public Integer getRouteTypeCode(String routeId) {
        return resolveRouteTypeCode(routeId);
    }
}
