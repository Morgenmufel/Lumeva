package lumeva.profileservice.domain.model;

public record Preferences(
        String theme,
        String timezone,
        String locale,
        boolean emailNotifications,
        boolean pushNotifications
) {
    public static Preferences defaultPreferences() {
        return new Preferences("dark", "ru", "UTC", true, true);
    }

}
