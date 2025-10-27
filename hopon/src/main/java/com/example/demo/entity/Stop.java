package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "route_stop_seq")
@IdClass(RouteStopSeqId.class)
@Getter
@Setter
public class Stop {

	@Id
	@Column(name = "route_id", length = 255, nullable = false)
	private String routeId;

	@Id
	@Column(name = "seq", nullable = false)
	private Integer seq;

	@Column(name = "route_name", length = 255) // NULL 허용
	private String routeName;

	@Column(name = "st_id", length = 32, nullable = false)
	private String stId;

	@Column(name = "ars_id", length = 255)     // NULL 허용
	private String arsId;

	@Column(name = "stop_name", length = 255)  // NULL 허용
	private String stopName;

	@Column(name = "lon", nullable = false)
	private Double lon;

	@Column(name = "lat", nullable = false)
	private Double lat;


}
