package lumeva.profileservice.infrastructure.security.resolver;

import lumeva.profileservice.domain.exceptions.MissingHeaderException;
import lumeva.profileservice.infrastructure.security.annotation.CurrentUserId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentUserIdArgumentResolverTest {

    @Mock
    private NativeWebRequest webRequest;

    @Mock
    private MethodParameter parameter;

    @InjectMocks
    private CurrentUserIdArgumentResolver resolver;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Должен доставать UUID из SecurityContextHolder, если он там задан")
    void resolveFromSecurityContext() {
        UUID userId = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, java.util.List.of())
        );

        Object result = resolver.resolveArgument(parameter, null, webRequest, null);

        assertThat(result).isEqualTo(userId);
    }

    @Test
    @DisplayName("Должен читать X-User-Id из заголовка, если SecurityContext пуст")
    void resolveFromHeader() {
        UUID userId = UUID.randomUUID();
        when(webRequest.getHeader("X-User-Id")).thenReturn(userId.toString());

        Object result = resolver.resolveArgument(parameter, null, webRequest, null);

        assertThat(result).isEqualTo(userId);
    }

    @Test
    @DisplayName("Должен бросать MissingHeaderException при невалидном UUID в заголовке")
    void throwOnInvalidUuidHeader() {
        when(webRequest.getHeader("X-User-Id")).thenReturn("not-a-uuid");

        assertThatThrownBy(() -> resolver.resolveArgument(parameter, null, webRequest, null))
                .isInstanceOf(MissingHeaderException.class)
                .hasMessageContaining("Invalid UUID");
    }
}
