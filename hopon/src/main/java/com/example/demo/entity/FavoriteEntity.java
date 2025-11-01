
package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

//import, 어노테이션 등 기존 동일
@Entity
@Table(name = "favorites",
     uniqueConstraints = @UniqueConstraint(name="uq_fav_unique",
             columnNames = {"user_num","route_id","direction","board_stop_id","dest_stop_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FavoriteEntity {
 @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
 private Long id;

 @ManyToOne(fetch = FetchType.LAZY, optional = false)
 @JoinColumn(name = "user_num", nullable = false)
 private UserEntity user;

 @Column(name="route_id", nullable=false, length=64)
 private String routeId;

 @Column(name="route_name",nullable = false, length = 20)
 private String routeName;

 @Column(name="direction", nullable=false, length=64)
 private String direction;

 @Column(name="board_stop_id", nullable=false, length=64)
 private String boardStopId;

 @Column(name="board_stop_name")
 private String boardStopName;

 @Column(name="board_ars_id", length=64)
 private String boardArsId;

 @Column(name="dest_stop_id", nullable=false, length=64)
 private String destStopId;

 @Column(name="dest_stop_name")
 private String destStopName;

 @Column(name="dest_ars_id", length=64)
 private String destArsId;

 // ⬇ 추가
 @Column(name = "bus_route_type")
 private Integer busRouteType;

 @Column(name = "route_type_name", length = 20)
 private String routeTypeName;

 @Column(name="created_at", nullable=false)
 private LocalDateTime createdAt;

 @Column(name="updated_at", nullable=false)
 private LocalDateTime updatedAt;

 @PrePersist
 public void prePersist() {
     this.createdAt = this.updatedAt = LocalDateTime.now();
 }
 @PreUpdate
 public void preUpdate() {
     this.updatedAt = LocalDateTime.now();
 }
}
