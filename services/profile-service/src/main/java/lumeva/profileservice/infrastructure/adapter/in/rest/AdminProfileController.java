package lumeva.profileservice.infrastructure.adapter.in.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lumeva.profileservice.application.dto.SanitizeProfileCommand;
import lumeva.profileservice.domain.port.in.GetProfileUseCase;
import lumeva.profileservice.domain.port.in.SanitizeProfileUseCase;
import lumeva.profileservice.infrastructure.adapter.in.rest.dto.AdminProfileResponse;
import lumeva.profileservice.infrastructure.adapter.in.rest.mapper.ProfileWebMapper;
import lumeva.profileservice.infrastructure.security.annotation.CurrentUserId;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("${lumeva.api.prefix}/admin/profiles")
@RequiredArgsConstructor
public class AdminProfileController {

    private final SanitizeProfileUseCase sanitizeProfileUseCase;
    private final GetProfileUseCase getProfileUseCase;
    private final ProfileWebMapper webMapper;

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('MODERATOR')") // ADMIN включает MODERATOR в Keycloak
    public ResponseEntity<AdminProfileResponse> getAdminProfileDetail(@PathVariable UUID userId) {
        return ResponseEntity.ok(webMapper.toAdminResponse(getProfileUseCase.getMyProfile(userId)));
    }

    @PostMapping("/{userId}/sanitize")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<Void> sanitizeProfile(
            @CurrentUserId UUID moderatorId,
            @PathVariable UUID userId,
            @Valid @RequestBody SanitizeProfileCommand command) {

        sanitizeProfileUseCase.sanitizeProfile(command.withContext(userId, moderatorId));
        return ResponseEntity.noContent().build();
    }

}
