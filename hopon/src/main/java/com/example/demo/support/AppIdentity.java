// src/main/java/com/example/demo/support/AppIdentity.java
package com.example.demo.support;

public record AppIdentity(
        String userId,
        String email,
        String role // USER | DRIVER | (선택) ADMIN
) {
    public boolean isUser()   { return "USER".equalsIgnoreCase(role); }
    public boolean isDriver() { return "DRIVER".equalsIgnoreCase(role); }
    public boolean isAdmin()  { return "ADMIN".equalsIgnoreCase(role); } // 선택 사용
}
