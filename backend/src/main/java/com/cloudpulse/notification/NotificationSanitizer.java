package com.cloudpulse.notification;

import java.util.regex.Pattern;

public class NotificationSanitizer {

    private static final Pattern SENSITIVE_KEY_VALUE = Pattern.compile(
            "(?i)(password|passwd|pwd|secret|auth|token|credential|api[_-]?key)\\s*[=:]\\s*[^\\s,;]+",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern BASIC_AUTH = Pattern.compile(
        "(?i)basic\\s+[a-z0-9+/=]+",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern EMAIL_IN_ERROR = Pattern.compile(
            "(?i)[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}"
    );

    private NotificationSanitizer() {
    }

    public static String sanitizeErrorMessage(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        return sanitizeErrorMessage(throwable.getMessage() != null ? throwable.getMessage() : throwable.getClass().getSimpleName());
    }

    public static String sanitizeErrorMessage(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) {
            return "Unknown error occurred";
        }

        // 1. Mask sensitive key-values (passwords, auth tokens, etc.)
        String sanitized = SENSITIVE_KEY_VALUE.matcher(rawMessage).replaceAll("$1=[REDACTED]");

        // 2. Mask Basic auth tokens
        sanitized = BASIC_AUTH.matcher(sanitized).replaceAll("Basic [REDACTED]");

        // 3. Mask email addresses inside error messages
        sanitized = EMAIL_IN_ERROR.matcher(sanitized).replaceAll("[REDACTED_EMAIL]");

        // 4. Truncate to maximum 500 characters
        if (sanitized.length() > 500) {
            sanitized = sanitized.substring(0, 497) + "...";
        }

        return sanitized.trim();
    }
}
