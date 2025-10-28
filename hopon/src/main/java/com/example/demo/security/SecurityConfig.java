// src/main/java/com/example/demo/security/SecurityConfig.java
package com.example.demo.security;

import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;

    @Bean
    public UserDetailsService userDetailsService() {
        return userid -> userRepository.findByUserid(userid)
                .map(u -> User.withUsername(u.getUserid())
                        .password(u.getPassword())
                        .authorities(u.getRole().name())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userid));
    }

    @Bean
    public AuthenticationProvider authenticationProvider(UserDetailsService uds) {
        DaoAuthenticationProvider p = new DaoAuthenticationProvider();
        p.setUserDetailsService(uds);
        p.setPasswordEncoder(passwordEncoder());
        return p;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public SecurityFilterChain filterChain(org.springframework.security.config.annotation.web.builders.HttpSecurity http) throws Exception {
        var jwtFilter = new JwtAuthenticationFilter(tokenProvider, userRepository);

        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(reg -> reg
                // preflight
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // ===== 인증 없이 접근 가능한 엔드포인트 =====
                .requestMatchers(HttpMethod.POST,
                        "/auth/register",
                        "/auth/login",
                        "/auth/refresh",
                        "/auth/logout",
                        "/auth/email/send-code",
                        "/auth/email/verify-code",
                        "/auth/find-id-after-verify",
                        "/auth/reset-password-after-verify",
                        "/auth/verify-pw-user"
                ).permitAll()
                .requestMatchers(HttpMethod.GET, "/auth/check").permitAll()

                // 공지
                .requestMatchers(HttpMethod.GET, "/api/notices/**").permitAll()

                // 버스/정류장
                .requestMatchers(HttpMethod.GET,
                        "/api/nearstations",
                        "/api/stationStop",
                        "/api/busLocation",
                        "/api/busStopList",
                        "/api/stations/nearby"
                ).permitAll()

                // ===== 문의(공개/소유자 분리) =====
                // 공개 목록/상세/첨부
                .requestMatchers(HttpMethod.GET, "/api/inquiries/public").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/inquiries/*/public").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/inquiries/*/attachments/*/public").permitAll()
                // 생성(회원/비회원 모두 가능 — X-User-* 헤더는 서버에서 요구)
                .requestMatchers(HttpMethod.POST, "/api/inquiries").permitAll()
                // 내 목록/내 상세/내 첨부 — 인증 필요(=X-User-* 헤더 필수)
                .requestMatchers(HttpMethod.GET, "/api/inquiries").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/inquiries/*").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/inquiries/*/attachments/*").authenticated()

                // 버스기사 운행
                .requestMatchers("/api/driver/**").hasRole("DRIVER")
                
                // 헬스체크 & 에러
                .requestMatchers("/actuator/health", "/error").permitAll()

                // ===== 역할/인증이 필요한 엔드포인트 =====
                .requestMatchers("/admin/**").hasRole("ADMIN")

                .requestMatchers("/users/**").authenticated()
                .requestMatchers("/api/reservations/**").authenticated()
                .requestMatchers("/api/favorites/**").authenticated()

                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider(userDetailsService()))
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, e) -> {
                    res.setStatus(401);
                    res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write("{\"error\":\"UNAUTHORIZED\"}");
                })
                .accessDeniedHandler((req, res, e) -> {
                    res.setStatus(403);
                    res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write("{\"error\":\"FORBIDDEN\"}");
                })
            );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOriginPatterns(List.of("*"));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setExposedHeaders(List.of("*"));
        cfg.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
