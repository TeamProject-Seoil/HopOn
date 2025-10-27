package com.example.demo.entity;

import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@EqualsAndHashCode
public class RouteStopSeqId implements Serializable {
	private String routeId; // route_id
	private Integer seq;    // seq
}