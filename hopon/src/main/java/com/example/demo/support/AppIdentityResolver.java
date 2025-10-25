package com.example.demo.support;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AppIdentityResolver {

    @Value("${jwt.secret}")
    private String secretKey; // application.yml에 jwt.secret 설정 필요

    public AppIdentity resolve(HttpServletRequest req) {
        String uid = header(req, "X-User-Id");
        String email = header(req, "X-User-Email");
        String role = header(req, "X-User-Role");

        // ✅ Authorization 헤더에서 토큰 추출
        String auth = req.getHeader("Authorization");
        if ((!StringUtils.hasText(uid) || !StringUtils.hasText(role)) && StringUtils.hasText(auth) && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);
            try {
                Claims claims = Jwts.parser()
                        .setSigningKey(secretKey.getBytes())
                        .parseClaimsJws(token)
                        .getBody();

                if (!StringUtils.hasText(uid))
                    uid = claims.get("sub", String.class); // 일반적으로 sub = userid
                if (!StringUtils.hasText(email))
                    email = claims.get("email", String.class);
                if (!StringUtils.hasText(role))
                    role = claims.get("role", String.class);
            } catch (Exception e) {
                // 로그만 남기고 계속 (익명 처리)
                System.err.println("[AppIdentityResolver] JWT 파싱 실패: " + e.getMessage());
            }
        }

        if (!StringUtils.hasText(role)) role = "USER";
        return new AppIdentity(uid, email, role.toUpperCase());
    }

    private String header(HttpServletRequest req, String key) {
        String v = req.getHeader(key);
        return StringUtils.hasText(v) ? v.trim() : null;
    }
}
