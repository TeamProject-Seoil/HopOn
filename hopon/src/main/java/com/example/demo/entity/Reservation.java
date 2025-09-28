package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "reservations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = true) // optional=false 권장
    @JoinColumn(name = "user_num", nullable = false)
    private UserEntity user;

    @Column(name = "route_id", nullable = false)
    private String routeId;

    private String direction;

    @Column(name = "board_stop_id")
    private String boardStopId;
    private String boardStopName;
    private String boardArsId;

    @Column(name = "dest_stop_id")
    private String destStopId;
    private String destStopName;
    private String destArsId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    @Builder.Default
    private ReservationStatus status = ReservationStatus.CONFIRMED;

    @CreationTimestamp
    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt; // ✨ 초기값 제거

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;   // ✨ 초기값 제거

    @PrePersist
    void prePersist() {
        if (status == null) status = ReservationStatus.CONFIRMED;
    }
}
