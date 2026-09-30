package lumeva.profileservice.infrastructure.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.MessageDigest;
import java.time.Instant;

public class InternalSecretFilter extends OncePerRequestFilter {

    private final String expectedSecret;

    public InternalSecretFilter(String expectedSecret) {
        this.expectedSecret = expectedSecret;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Пропускаем health-пробы Kubernetes/Actuator без секрета
        if (request.getRequestURI().startsWith("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }

        String headerSecret = request.getHeader("X-Internal-Secret");

        if (headerSecret == null || !isSecretValid(headerSecret, expectedSecret)) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.getWriter().write("""
                {
                    "type": "https://lumeva.com/errors/invalid-internal-secret",
                    "title": "Forbidden",
                    "status": 403,
                    "detail": "Access Denied: Request must originate from API Gateway",
                    "timestamp": "%s"
                }
                """.formatted(Instant.now()));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isSecretValid(String provided, String expected) {
        return MessageDigest.isEqual(provided.getBytes(), expected.getBytes());
    }
}