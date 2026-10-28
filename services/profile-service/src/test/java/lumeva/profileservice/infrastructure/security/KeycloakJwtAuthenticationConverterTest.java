package lumeva.profileservice.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakJwtAuthenticationConverterTest {

    private final KeycloakJwtAuthenticationConverter converter = new KeycloakJwtAuthenticationConverter();

    @Test
    void mapsRealmRolesToAuthoritiesAndUsesSubjectAsName() {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "RS256")
                .subject("a34a043a-82a9-4984-95e8-6dcd1e4ef1ad")
                .claim("realm_access", Map.of("roles", List.of("USER", "MODERATOR")))
                .build();

        var authentication = converter.convert(jwt);

        assertThat(authentication.getName()).isEqualTo("a34a043a-82a9-4984-95e8-6dcd1e4ef1ad");
        assertThat(authentication.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_MODERATOR");
    }

    @Test
    void tokenWithoutRealmAccessHasNoAuthorities() {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "RS256").subject("id").build();

        assertThat(converter.convert(jwt).getAuthorities()).isEmpty();
    }
}
