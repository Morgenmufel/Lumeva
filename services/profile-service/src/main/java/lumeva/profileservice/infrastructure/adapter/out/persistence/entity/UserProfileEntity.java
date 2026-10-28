package lumeva.profileservice.infrastructure.adapter.out.persistence.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "personal_info", nullable = false)
    private PersonalInfoJson personalInfo;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "preferences", nullable = false)
    private PreferencesJson preferences;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "avatar", nullable = false)
    private AvatarJson avatar;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Оптимистическая блокировка: UPDATE ... WHERE version = ?.
     * Обёртка Long, а не long: null означает «новая сущность», и Spring Data
     * делает persist без лишнего SELECT, как было бы при merge.
     */
    @Version
    private Long version;

    public record PersonalInfoJson(
            String firstName,
            String lastName,
            String displayName,
            String bio,
            Map<String, String> socialLinks
    ) {}

    public record PreferencesJson(
            String theme,
            String locale,
            String timezone,
            boolean emailNotifications,
            boolean pushNotifications
    ) {}

    public record AvatarJson(
            String originalUrl,
            String thumbnailUrl,
            Instant uploadedAt
    ) {}
}
