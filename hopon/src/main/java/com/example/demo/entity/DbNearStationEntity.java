package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.locationtech.jts.geom.Point;

@Getter // Lombok Getter 추가
@Entity
@Table(name = "stops")
public class DbNearStationEntity {

    @Id
    @Column(name = "st_id", nullable = false)
    private String stId;

    @Column(name = "ars_id")
    private String arsId;

    @Column(name = "name")
    private String name;

    @Column(name = "lon", nullable = false)
    private Double lon;

    @Column(name = "lat", nullable = false)
    private Double lat;

    @Column(name = "geom", nullable = false, columnDefinition = "POINT SRID 4326")
    private Point geom;

}
