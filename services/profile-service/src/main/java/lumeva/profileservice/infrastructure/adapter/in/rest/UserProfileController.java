package lumeva.profileservice.infrastructure.adapter.in.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lumeva.profileservice.application.dto.*;
import lumeva.profileservice.domain.port.in.GetProfileUseCase;
import lumeva.profileservice.domain.port.in.UpdateProfileUseCase;
import lumeva.profileservice.infrastructure.adapter.in.rest.dto.MyProfileResponse;
import lumeva.profileservice.infrastructure.adapter.in.rest.dto.PublicProfileResponse;
import lumeva.profileservice.infrastructure.adapter.in.rest.mapper.ProfileWebMapper;
import lumeva.profileservice.infrastructure.security.annotation.CurrentUserId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("${lumeva.api.prefix}/profiles")
@RequiredArgsConstructor
public class UserProfileController {

    private final GetProfileUseCase getProfileUseCase;
    private final UpdateProfileUseCase updateProfileUseCase;
    private final ProfileWebMapper webMapper;

    @GetMapping("/me")
    public ResponseEntity<MyProfileResponse> getMyProfile(@CurrentUserId UUID userId) {
        return ResponseEntity.ok(webMapper.toMyResponse(getProfileUseCase.getMyProfile(userId)));
    }

    @GetMapping("/public/{userId}")
    public ResponseEntity<PublicProfileResponse> getPublicProfile(@PathVariable UUID userId) {
        return ResponseEntity.ok(webMapper.toPublicResponse(getProfileUseCase.getPublicProfile(userId)));
    }

    @PostMapping("/batch")
    public ResponseEntity<List<PublicProfileResponse>> getProfilesBatch(
            @RequestBody Set<UUID> targetUserIds) {

        return ResponseEntity.ok(getProfileUseCase.getProfilesBatch(targetUserIds)
                .stream()
                .map(webMapper::toPublicResponse)
                .toList());
    }

    @PutMapping("/me/personal-info")
    public ResponseEntity<MyProfileResponse> updatePersonalInfo(
            @CurrentUserId UUID userId,
            @Valid @RequestBody UpdatePersonalInfoCommand command) {

        return ResponseEntity
                .ok(webMapper.toMyResponse(updateProfileUseCase
                        .updatePersonalInfo(command.withUserId(userId))));
    }

    @PutMapping("/me/preferences")
    public ResponseEntity<MyProfileResponse> updatePreferences(
            @CurrentUserId UUID userId,
            @Valid @RequestBody UpdatePreferencesCommand command) {

        return ResponseEntity.ok(webMapper.toMyResponse(updateProfileUseCase
                .updatePreferences(command.withUserId(userId))));
    }

    @PutMapping("/me/avatar")
    public ResponseEntity<MyProfileResponse> updateAvatar(
            @CurrentUserId UUID userId,
            @Valid @RequestBody UpdateAvatarCommand command) {

        return ResponseEntity.ok(webMapper.toMyResponse(updateProfileUseCase
                .updateAvatar(command.withUserId(userId))));
    }

    @DeleteMapping("/me/avatar")
    public ResponseEntity<MyProfileResponse> deleteAvatar(@CurrentUserId UUID userId) {
        return ResponseEntity.ok(webMapper.toMyResponse(updateProfileUseCase
                .deleteAvatar(DeleteAvatarCommand.of(userId))));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMyProfile(@CurrentUserId UUID userId) {
        updateProfileUseCase.deleteProfile(DeleteProfileCommand.of(userId));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/me/anonymize")
    public ResponseEntity<MyProfileResponse> anonymizeProfile(
            @CurrentUserId UUID userId,
            @Valid @RequestBody AnonymizeProfileCommand command) {

        return ResponseEntity.ok(webMapper.toMyResponse(updateProfileUseCase
                .anonymizeProfile(command.withUserId(userId))));
    }
}