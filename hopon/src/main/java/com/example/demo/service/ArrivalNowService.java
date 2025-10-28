// src/main/java/com/example/demo/service/ArrivalNowService.java
package com.example.demo.service;

import com.example.demo.dto.*;
import lombok.RequiredArgsConstructor;
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

    /** 
     * 주 버전:
     * - 노선 정류장 목록(getStaionByRoute)로 현재/다음 정류장 이름 산출
     * - 현재 정류장의 도착정보(getStationByUid)에서 plateNo 매칭 → ETA(초) 산출
     */
    public ArrivalNowResponse build(String routeId, BusLocationDto bus, String fallbackPlainNo) {
        // 1) 노선의 정류장 목록
        List<BusStopListDto> stops = busStopListService.getStaionByRoute(routeId);
        if (stops == null || stops.isEmpty()) {
            return empty();
        }

        // 2) sectOrd 기반 현재 seq 추정 (없으면 좌표 근접)
        Integer currSeq = seqFromSectOrd(bus, stops);
        if (currSeq == null) {
            currSeq = nearestSeqByCoord(bus, stops);
        }

        // 3) 현재/다음 정류장 이름/ARS 추출
        BusStopListDto current = findBySeq(stops, currSeq);
        BusStopListDto next    = findBySeq(stops, currSeq == null ? null : currSeq + 1);
        String currentName = current != null ? nz(current.getStationNm(), "-") : "-";
        String nextName    = next    != null ? nz(next.getStationNm(), "-")    : "-";

        // 4) ETA: 현재 정류장 ARS로 도착정보 조회 → plainNo 매칭
        Integer etaSec = null;
        String arsForEta = current != null ? current.getArsId() : null;

        if (StringUtils.hasText(arsForEta)) {
            List<StationStopListDto> arrivals = stationStopListService.getStationByUid(arsForEta);
            if (arrivals != null && !arrivals.isEmpty()) {
                String targetPlate = normalizePlate(
                        StringUtils.hasText(bus.getPlainNo()) ? bus.getPlainNo() : fallbackPlainNo
                );

                // 같은 노선의 도착 리스트 중 번호판/노선으로 매칭
                // (API가 당도하는 버스들을 여러 줄로 줄 수 있어 loop로 매칭)
                for (StationStopListDto row : arrivals) {
                    // 노선 필터
                    if (!routeId.equals(row.getBusRouteId())) continue;

                    // arrmsg1/2는 "도착" 문구이며, 번호판을 직접 제공하지 않음.
                    // 일부 구현에서는 정류장 API가 차량명세를 따로 주지 않기 때문에
                    // 같은 정류장의 '도착 리스트'와 현재 버스 위치(sectOrd/좌표)를 교차해 근사해야 함.
                    // 여기서는 '노선 동일 + 현재 정류장'으로 보고, 가장 이른 ETA를 사용.
                    Integer e1 = ArrmsgParser.toSeconds(row.getArrmsg1());
                    Integer e2 = ArrmsgParser.toSeconds(row.getArrmsg2());

                    // targetPlate를 활용할 수 있는 경우가 드뭄 → plate 매칭 불가 시 가장 짧은 ETA 선택
                    Integer candidate = minIgnoreNull(e1, e2);
                    if (candidate != null) {
                        etaSec = etaSec == null ? candidate : Math.min(etaSec, candidate);
                    }
                }
            }
        }

        return ArrivalNowResponse.builder()
                .currentStopName(currentName)
                .nextStopName(nextName)
                .etaSec(etaSec)
                .build();
    }

    // ====== 보조 메서드들 ======

    private ArrivalNowResponse empty() {
        return ArrivalNowResponse.builder()
                .currentStopName("-")
                .nextStopName("-")
                .etaSec(null)
                .build();
    }

    private Integer seqFromSectOrd(BusLocationDto bus, List<BusStopListDto> stops) {
        try {
            String so = bus.getSectOrd();
            if (!StringUtils.hasText(so)) return null;
            int seq = Integer.parseInt(so);
            // 범위 체크
            int maxSeq = stops.stream().map(BusStopListDto::getSeq).max(Comparator.naturalOrder()).orElse(0);
            if (seq < 1 || seq > maxSeq) return null;
            return seq;
        } catch (Exception e) {
            return null;
        }
    }

    private Integer nearestSeqByCoord(BusLocationDto bus, List<BusStopListDto> stops) {
        if (bus.getGpsY() == null || bus.getGpsX() == null) return null;
        double lat = bus.getGpsY();
        double lon = bus.getGpsX();
        double best = Double.MAX_VALUE;
        Integer bestSeq = null;
        for (BusStopListDto s : stops) {
            if (s.getGpsY() == null || s.getGpsX() == null) continue;
            double d2 = dist2(lat, lon, s.getGpsY(), s.getGpsX());
            if (d2 < best) {
                best = d2;
                bestSeq = s.getSeq();
            }
        }
        return bestSeq;
    }

    private double dist2(double lat1, double lon1, double lat2, double lon2) {
        double dy = lat1 - lat2, dx = lon1 - lon2;
        return dx*dx + dy*dy;
    }

    private BusStopListDto findBySeq(List<BusStopListDto> stops, Integer seq) {
        if (seq == null) return null;
        for (BusStopListDto s : stops) {
            if (Objects.equals(s.getSeq(), seq)) return s;
        }
        return null;
    }

    private Integer minIgnoreNull(Integer a, Integer b) {
        if (a == null) return b;
        if (b == null) return a;
        return Math.min(a, b);
    }

    private String nz(String s, String d) { return (s == null || s.isBlank()) ? d : s; }

    private String normalizePlate(String s) {
        if (s == null) return "";
        return s.replaceAll("[^0-9가-힣A-Za-z]", "").toUpperCase();
    }
}
