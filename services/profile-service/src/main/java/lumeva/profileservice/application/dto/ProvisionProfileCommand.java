package lumeva.profileservice.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ProvisionProfileCommand(
        @NotNull(message = "User ID is required")
        UUID userId,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email
) {}