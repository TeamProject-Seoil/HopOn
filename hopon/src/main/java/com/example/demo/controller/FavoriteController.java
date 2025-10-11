// FavoriteController.java
package com.example.demo.controller;

import com.example.demo.dto.FavoriteCreateRequest;
import com.example.demo.dto.FavoriteResponse;
import com.example.demo.entity.UserEntity;
import com.example.demo.service.FavoriteService;
import com.example.demo.support.AuthUserResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final AuthUserResolver authUserResolver;

    @PostMapping
    public ResponseEntity<FavoriteResponse> add(Authentication authentication,
                                                @RequestBody FavoriteCreateRequest req) {
        UserEntity user = authUserResolver.requireUser(authentication);
        return ResponseEntity.ok(favoriteService.add(user, req));
    }

    @GetMapping("/top3")
    public ResponseEntity<List<FavoriteResponse>> top3(Authentication authentication) {
        UserEntity user = authUserResolver.requireUser(authentication);
        return ResponseEntity.ok(favoriteService.top3(user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(Authentication authentication, @PathVariable Long id) {
        UserEntity user = authUserResolver.requireUser(authentication);
        favoriteService.remove(user, id);
        return ResponseEntity.noContent().build();
    }
}
