package com.example.demo.entity;

public enum TargetRole {
    ALL, USER, DRIVER, ADMIN;

    public static TargetRole from(String raw) {
        if (raw == null || raw.isBlank()) return ALL;
        String s = raw.trim().toUpperCase();
        if (s.startsWith("ROLE_")) s = s.substring(5); // ✅ ROLE_ 제거
        return switch (s) {
            case "ALL" -> ALL;
            case "USER" -> USER;
            case "DRIVER" -> DRIVER;
            case "ADMIN" -> ADMIN;
            default -> ALL;
        };
    }
}
