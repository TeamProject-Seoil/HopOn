// AuthUserResolver.java
package com.example.demo.support;

import com.example.demo.entity.UserEntity;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class AuthUserResolver {
    private final UserRepository userRepository;

    public UserEntity requireUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");

        Object p = authentication.getPrincipal();
        if (p instanceof UserEntity u) return u;

        if (p instanceof UserDetails ud) {
            return userRepository.findByUserid(ud.getUsername())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND"));
        }
        if (p instanceof String username) {
            if ("anonymousUser".equals(username))
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "ANONYMOUS");
            return userRepository.findByUserid(username)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND"));
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "UNSUPPORTED_PRINCIPAL");
    }
}
