// src/main/java/com/example/demo/entity/DriverLicenseEntity.java
package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "driver_license",
    indexes = {
        @Index(name = "idx_dl_user",        columnList = "user_num",       unique = true),
        @Index(name = "idx_dl_license_no",  columnList = "license_number", unique = true)
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DriverLicenseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "license_id")
    private Long licenseId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "user_num",
        nullable = false,
        unique = true,
        foreignKey = @ForeignKey(name = "fk_driver_license_user")
    )
    private UserEntity user;

    @Column(name = "license_number", nullable = false, length = 50, unique = true)
    private String licenseNumber;

    @Column(name = "acquired_date", nullable = false)
    private LocalDate acquiredDate;

    @Lob
    @Column(name = "license_image", columnDefinition = "LONGBLOB")
    private byte[] licenseImage;

    // DB DEFAULT CURRENT_TIMESTAMP 읽기 전용 매핑
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
