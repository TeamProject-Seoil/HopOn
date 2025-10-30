// src/main/java/com/example/demo/controller/ReservationBusController.java
package com.example.demo.controller;

import com.example.demo.dto.BusLocationDto;
import com.example.demo.dto.DriverLocationDto;
import com.example.demo.entity.ReservationEntity;
import com.example.demo.repository.DriverOperationRepository;
import com.example.demo.repository.ReservationRepository;
import com.example.demo.service.BusLocationService;
import com.example.demo.support.AuthUserResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

//src/main/java/com/example/demo/controller/ReservationBusController.java
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/reservations")
public class ReservationBusController {

 private final ReservationRepository reservationRepository;
 private final DriverOperationRepository driverOperationRepository; // ✅ 추가
 private final BusLocationService busLocationService;
 private final AuthUserResolver auth;

 @GetMapping("/{id}/location")
 public ResponseEntity<DriverLocationDto> currentLocation(Authentication a, @PathVariable Long id){
     var user = auth.requireUser(a);
     var r = reservationRepository.findById(id).orElse(null);
     if (r == null || !r.getUser().getUserNum().equals(user.getUserNum())) {
         return ResponseEntity.notFound().build();
     }

     // 1) HopOn 운행과 연결된 경우 → 운영 DB의 최신 하트비트 좌표 리턴
     if (r.getOperationId() != null) {
         var op = driverOperationRepository.findById(r.getOperationId()).orElse(null);
         if (op != null && op.getLastLat() != null && op.getLastLon() != null) {
             var dto = DriverLocationDto.builder()
                     .operationId(op.getId())
                     .lat(op.getLastLat())
                     .lon(op.getLastLon())
                     .updatedAtIso(op.getUpdatedAt() == null ? null :
                             op.getUpdatedAt().atOffset(java.time.ZoneOffset.UTC).toString())
                     .stale(false)
                     .build();
             return ResponseEntity.ok(dto); // ✅ 여기서 바로 종료
         }
         // 좌표가 아직 없으면 아래 공공데이터 fallback으로 진행
     }

     // 2) 공공데이터 fallback (현재 코드 유지)
     var list = busLocationService.getBusPosByRtid(r.getRouteId());
     var match = list.stream().filter(b ->
             norm(b.getPlainNo()).equals(norm(r.getApiPlainNo()))
          || (r.getApiVehId()!=null && r.getApiVehId().equals(b.getVehId()))
     ).findFirst().orElse(null);

     if (match == null) return ResponseEntity.noContent().build();

     var dto = DriverLocationDto.builder()
             .operationId(r.getOperationId())
             .lat(match.getGpsY())
             .lon(match.getGpsX())
             .updatedAtIso(match.getDataTm())
             .stale(false)
             .build();
     return ResponseEntity.ok(dto);
 }

 private String norm(String s){ return s==null?"":s.replaceAll("[^0-9가-힣A-Za-z]","").toUpperCase(); }
}
