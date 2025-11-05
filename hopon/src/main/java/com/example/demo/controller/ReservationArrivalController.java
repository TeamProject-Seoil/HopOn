// src/main/java/com/example/demo/controller/ReservationArrivalController.java
package com.example.demo.controller;

import com.example.demo.dto.ReservationArrivalStateResponse;
import com.example.demo.service.ReservationArrivalService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationArrivalController {

    private final ReservationArrivalService reservationArrivalService;

    @GetMapping("/{id}/arrival-state")
    public ReservationArrivalStateResponse getArrivalState(Authentication auth,
                                                           @PathVariable Long id) {
        return reservationArrivalService.getArrivalState(auth, id);
    }
}
