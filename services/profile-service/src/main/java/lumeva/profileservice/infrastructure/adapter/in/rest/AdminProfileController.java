package lumeva.profileservice.infrastructure.adapter.in.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lumeva.profileservice.application.dto.*;
import lumeva.profileservice.domain.port.in.GetProfileUseCase;
import lumeva.profileservice.domain.port.in.ModerateProfileUseCase;
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

    private final ModerateProfileUseCase moderateProfileUseCase;
    private final GetProfileUseCase getProfileUseCase;
    private final ProfileWebMapper webMapper;

    @GetMapping("/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<AdminProfileResponse> getAdminProfileDetail(@PathVariable UUID userId) {
        return ResponseEntity.ok(webMapper.toAdminResponse(getProfileUseCase.getMyProfile(userId)));
    }

    @PostMapping("/{userId}/ban")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> banUser(
            @CurrentUserId UUID adminId,
            @PathVariable UUID userId,
            @Valid @RequestBody BanUserCommand command) {

        moderateProfileUseCase.banUser(command.withContext(userId, adminId));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{userId}/unban")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> unbanUser(
            @CurrentUserId UUID adminId,
            @PathVariable UUID userId,
            @Valid @RequestBody UnbanUserCommand command) {

        moderateProfileUseCase.unbanUser(command.withContext(userId, adminId));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{userId}/suspend")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> suspendUser(
            @CurrentUserId UUID adminId,
            @PathVariable UUID userId,
            @Valid @RequestBody SuspendUserCommand command) {

        moderateProfileUseCase.suspendUser(command.withContext(userId, adminId));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{userId}/flag")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<Void> flagUser(
            @CurrentUserId UUID moderatorId,
            @PathVariable UUID userId,
            @Valid @RequestBody FlagUserCommand command) {

        moderateProfileUseCase.flagUser(command.withContext(userId, moderatorId));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{userId}/sanitize")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<Void> sanitizeProfile(
            @CurrentUserId UUID moderatorId,
            @PathVariable UUID userId,
            @Valid @RequestBody SanitizeProfileCommand command) {

        moderateProfileUseCase.sanitizeProfile(command.withContext(userId, moderatorId));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{userId}/notes")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<Void> addModeratorNote(
            @CurrentUserId UUID moderatorId,
            @PathVariable UUID userId,
            @Valid @RequestBody AddModeratorNoteCommand command) {

        moderateProfileUseCase.addModeratorNote(command.withContext(userId, moderatorId));
        return ResponseEntity.noContent().build();
    }
}