// config/SecurityConfig.java

package com.LockSaveApplication.config;

import com.LockSaveApplication.security.UserDetailsServiceImpl;
import com.LockSaveApplication.security.filter.RateLimitFilter;
import com.LockSaveApplication.security.filter.RequestSanitizationFilter;
import com.LockSaveApplication.security.jwt.JwtAuthenticationEntryPoint;
import com.LockSaveApplication.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter     jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final UserDetailsServiceImpl      userDetailsService;
    private final RateLimitFilter             rateLimitFilter;
    private final RequestSanitizationFilter   requestSanitizationFilter;

    // Read CORS config directly from properties
    // No CorsConfigurationSource bean injected — avoids the ambiguity
    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Value("${app.cors.allowed-methods}")
    private String allowedMethods;

    @Value("${app.cors.allow-credentials}")
    private boolean allowCredentials;

    @Value("${app.cors.max-age}")
    private long maxAge;

    // ── Public endpoints ──────────────────────────────────────────────────────
    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/auth/**",
            "/api/v1/webhooks/**",
            "/actuator/health"
    };

    // ── Security filter chain ─────────────────────────────────────────────────
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            // ── Disable CSRF — stateless JWT, no session ─────────────────────
            .csrf(AbstractHttpConfigurer::disable)

            // ── CORS — built inline, no bean injection ambiguity ─────────────
            .cors(cors -> cors.configurationSource(buildCorsSource()))

            // ── Stateless session ────────────────────────────────────────────
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // ── 401 JSON on auth failure ─────────────────────────────────────
            .exceptionHandling(ex ->
                ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))

            // ── Endpoint authorization ────────────────────────────────────────
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                .anyRequest().authenticated()
            )

            // ── Security response headers ─────────────────────────────────────
            .headers(headers -> headers
                .frameOptions(frame -> frame.deny())
                .xssProtection(xss -> xss.disable())
                .contentSecurityPolicy(csp ->
                    csp.policyDirectives(
                        "default-src 'self'; " +
                        "script-src 'self'; " +
                        "style-src 'self'; " +
                        "img-src 'self' data:; " +
                        "font-src 'self'; " +
                        "connect-src 'self'; " +
                        "frame-ancestors 'none';"
                    )
                )
                .referrerPolicy(referrer ->
                    referrer.policy(
                        ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                .contentTypeOptions(contentType -> {})
            )

            // ── Authentication provider ───────────────────────────────────────
            .authenticationProvider(authenticationProvider())

            // ── Filter order ──────────────────────────────────────────────────
            // 1. Rate limit  — reject floods before any processing
            // 2. Sanitize    — strip XSS from params and headers
            // 3. JWT         — validate token, set security context
            .addFilterBefore(rateLimitFilter,
                    UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(requestSanitizationFilter,
                    UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter,
                    UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ── CORS source — built here, not injected ────────────────────────────────
    private CorsConfigurationSource buildCorsSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOriginPatterns(
                Arrays.stream(allowedOrigins.split(","))
                        .map(String::trim)
                        .toList()
        );

        config.setAllowedMethods(
                Arrays.stream(allowedMethods.split(","))
                        .map(String::trim)
                        .toList()
        );

        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(allowCredentials);
        config.setMaxAge(maxAge);
        config.setExposedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "X-Total-Count",
                "X-Rate-Limit-Remaining"
        ));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    // ── Authentication provider ───────────────────────────────────────────────
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    // ── Authentication manager ────────────────────────────────────────────────
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // ── Password encoder ─────────────────────────────────────────────────────
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}