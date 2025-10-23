// src/main/java/com/example/demo/config/SecurityBeans.java
package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCrypt;

@Configuration
public class SecurityBeans {

    /** 간단한 래퍼 — 필요 시 Spring Security PasswordEncoder 대체 가능 */
    @Bean
    public PasswordHasher passwordHasher() {
        return new PasswordHasher();
    }

    public static class PasswordHasher {
        public String hash(String raw) {
            return BCrypt.hashpw(raw, BCrypt.gensalt());
        }
        public boolean matches(String raw, String hashed) {
            if (hashed == null || hashed.isBlank()) return false;
            return BCrypt.checkpw(raw, hashed);
        }
    }
}
