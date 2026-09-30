package lumeva.profileservice.infrastructure.adapter.in.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lumeva.profileservice.application.dto.ProvisionProfileCommand;
import lumeva.profileservice.domain.port.in.ProvisionProfileUseCase;
import lumeva.profileservice.infrastructure.adapter.in.rest.dto.MyProfileResponse;
import lumeva.profileservice.infrastructure.adapter.in.rest.mapper.ProfileWebMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${lumeva.api.prefix}/internal/profiles")
@RequiredArgsConstructor
public class InternalProfileController {

    private final ProvisionProfileUseCase provisionProfileUseCase;
    private final ProfileWebMapper webMapper;

    @PostMapping
    public ResponseEntity<MyProfileResponse> provisionProfile(
            @Valid @RequestBody ProvisionProfileCommand command) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(webMapper.toMyResponse(provisionProfileUseCase.createInitialProfile(command)));
    }
}
