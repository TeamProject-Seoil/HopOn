// src/main/java/com/example/demo/support/AppIdentityResolver.java
package com.example.demo.support;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AppIdentityResolver {
    public AppIdentity resolve(HttpServletRequest req) {
        String uid   = header(req, "X-User-Id");
        String email = header(req, "X-User-Email");
        String role  = header(req, "X-User-Role");
        if (!StringUtils.hasText(role)) role = "USER";
        return new AppIdentity(uid, email, role.toUpperCase());
    }

    private String header(HttpServletRequest req, String key){
        String v = req.getHeader(key);
        return StringUtils.hasText(v) ? v.trim() : null;
    }
}
