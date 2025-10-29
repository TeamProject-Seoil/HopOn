package com.example.demo.service;

//RouteTypeMapper.java
public final class RouteTypeMapper {
 private RouteTypeMapper(){}

 public static String toLabel(Integer code) {
     if (code == null) return "기타";
     return switch (code) {
         case 1  -> "공항";
         case 2  -> "마을";
         case 3  -> "간선";
         case 4  -> "지선";
         case 5  -> "순환";
         case 6  -> "광역";
         case 7  -> "인천";
         case 8  -> "경기";
         case 9  -> "폐지";
         case 0  -> "공용";
         default -> "기타";
     };
 }
}

