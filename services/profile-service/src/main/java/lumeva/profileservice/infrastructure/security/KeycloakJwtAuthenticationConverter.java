package lumeva.profileservice.infrastructure.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Keycloak кладёт роли realm в claim {"realm_access": {"roles": [...]}}, а Spring по умолчанию
 * читает только "scope". Конвертер превращает роли в ROLE_* для hasRole(...).
 *
 * Иерархия ролей уже раскрыта Keycloak: роль ADMIN составная и включает MODERATOR и USER,
 * поэтому в токене админа есть все три, и отдельная RoleHierarchy в сервисе не нужна.
 *
 * Имя принципала — sub (id пользователя в Keycloak), а не preferred_username:
 * id стабилен, ник может поменяться.
 */
public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String REALM_ACCESS = "realm_access";
    private static final String ROLES = "roles";

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        var authorities = extractRealmRoles(jwt).stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }

    private static Collection<String> extractRealmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap(REALM_ACCESS);
        if (realmAccess == null || !(realmAccess.get(ROLES) instanceof Collection<?> roles)) {
            return List.of();
        }
        return roles.stream().map(Object::toString).toList();
    }
}
