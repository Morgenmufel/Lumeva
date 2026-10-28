package lumeva.profileservice.domain.model;

/**
 * Статус профиля.
 * RESTRICTED — копия санкции из Moderation (бан или заморозка): Profile важно только то,
 * что редактировать профиль нельзя. Причина и срок санкции хранятся в Moderation (ADR-001).
 */
public enum ProfileStatus {
    ACTIVE,
    RESTRICTED,
    DELETED,
    ANONYMIZED
}
