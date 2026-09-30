package lumeva.profileservice.infrastructure.security;


import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("profileSecurity")
public class ProfileSecurityEvaluator {

    public boolean isSelf(Authentication authentication, UUID targetUserId) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return false;
        }
        if (authentication.getPrincipal() instanceof UUID currentUserId) {
            return currentUserId.equals(targetUserId);
        }
        return authentication.getPrincipal().toString().equalsIgnoreCase(targetUserId.toString());
    }

    public boolean hasRole(Authentication authentication, String roleName) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        String expectedRole = roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName;
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equalsIgnoreCase(expectedRole));
    }

    public boolean isSelfOrAdmin(Authentication authentication, UUID targetUserId) {
        return isSelf(authentication, targetUserId) || hasRole(authentication, "ADMIN");
    }
}