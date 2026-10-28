package lumeva.profileservice.domain.exceptions;

/** Данные профиля нарушают правила домена (например, слишком длинное bio). Отдаётся как 400. */
public class InvalidProfileDataException extends DomainException {
    public InvalidProfileDataException(String message) {
        super(message);
    }
}
