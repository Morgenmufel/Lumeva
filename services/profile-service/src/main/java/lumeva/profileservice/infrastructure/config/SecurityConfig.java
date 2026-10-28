package lumeva.profileservice.infrastructure.config;

import lombok.RequiredArgsConstructor;
import lumeva.profileservice.infrastructure.security.KeycloakJwtAuthenticationConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.time.Instant;

/**
 * Каждый запрос проверяется по JWT от Keycloak (ADR-002): подпись по ключам JWKS,
 * issuer и срок действия. Gateway проверяет токен тоже, но сервис ему не доверяет вслепую.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("${lumeva.api.prefix:/api/v1}")
    private String apiPrefix;

    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CORS обрабатывает Gateway; CSRF не нужен — сервис без cookie-сессий, только Bearer
                .cors(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(problemAuthenticationEntryPoint())
                        .accessDeniedHandler(problemAccessDeniedHandler()))
                .authorizeHttpRequests(auth -> auth
                        // наружу только health и info; метрики — для внутренней сети, позже за отдельным портом
                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.GET, apiPrefix + "/profiles/public/**").permitAll()
                        // свой профиль — только у пользователей; сервисные аккаунты сюда не ходят
                        .requestMatchers(apiPrefix + "/profiles/me", apiPrefix + "/profiles/me/**").hasRole("USER")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(new KeycloakJwtAuthenticationConverter()))
                        .authenticationEntryPoint(problemAuthenticationEntryPoint()));

        return http.build();
    }

    private AuthenticationEntryPoint problemAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(),
                    problem(HttpStatus.UNAUTHORIZED, "Unauthorized", "Valid access token required", "unauthorized"));
        };
    }

    private AccessDeniedHandler problemAccessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(),
                    problem(HttpStatus.FORBIDDEN, "Forbidden", "Insufficient permissions", "access-denied"));
        };
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail, String code) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        problemDetail.setType(URI.create("https://lumeva.com/errors/" + code));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }
}
