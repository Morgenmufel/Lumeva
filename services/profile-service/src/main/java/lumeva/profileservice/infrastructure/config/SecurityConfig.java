package lumeva.profileservice.infrastructure.config;


import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.time.Instant;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("${lumeva.internal-secret}")
    private String internalSecret;

    @Value("${lumeva.api.prefix:/api/v1}")
    private String apiPrefix;

    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        InternalSecretFilter internalSecretFilter = new InternalSecretFilter(internalSecret);
        UserHeaderFilter userHeaderFilter = new UserHeaderFilter();

        http
                // CORS отключаем полностью, так как префлайты (OPTIONS) обрабатывает API Gateway
                .cors(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(customAuthenticationEntryPoint())
                        .accessDeniedHandler(customAccessDeniedHandler())
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers(apiPrefix + "/internal/**").permitAll() // Защищено через InternalSecretFilter
                        .requestMatchers(apiPrefix + "/profiles/public/**").permitAll()
                        .anyRequest().authenticated()
                )
                // Строгий порядок: Сначала проверяем секрет Gateway, затем читаем пользователя
                .addFilterBefore(internalSecretFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(userHeaderFilter, InternalSecretFilter.class);

        return http.build();
    }

    private AuthenticationEntryPoint customAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

            ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Authentication required");
            problemDetail.setTitle("Unauthorized");
            problemDetail.setType(URI.create("https://lumeva.com/errors/unauthorized"));
            problemDetail.setProperty("timestamp", Instant.now());

            objectMapper.writeValue(response.getOutputStream(), problemDetail);
        };
    }

    private AccessDeniedHandler customAccessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

            ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Insufficient permissions");
            problemDetail.setTitle("Forbidden");
            problemDetail.setType(URI.create("https://lumeva.com/errors/access-denied"));
            problemDetail.setProperty("timestamp", Instant.now());

            objectMapper.writeValue(response.getOutputStream(), problemDetail);
        };
    }
}