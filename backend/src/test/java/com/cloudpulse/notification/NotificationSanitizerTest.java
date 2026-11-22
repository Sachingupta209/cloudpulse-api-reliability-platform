package com.cloudpulse.notification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationSanitizerTest {

    @Test
    @DisplayName("Should redact passwords, secrets, tokens, and basic auth credentials")
    void shouldRedactSensitiveInformation() {
        String errorWithPassword = "Authentication failed for user=admin with password=SuperSecretPassword123! at host:1025";
        String sanitized1 = NotificationSanitizer.sanitizeErrorMessage(errorWithPassword);
        assertThat(sanitized1).doesNotContain("SuperSecretPassword123!");
        assertThat(sanitized1).contains("password=[REDACTED]");

        String errorWithBasicAuth = "Server rejected header Authorization: Basic dXNlcjpwYXNzd29yZA==";
        String sanitized2 = NotificationSanitizer.sanitizeErrorMessage(errorWithBasicAuth);
        assertThat(sanitized2).doesNotContain("dXNlcjpwYXNzd29yZA==");
        assertThat(sanitized2).contains("Basic [REDACTED]");

        String errorWithApiKey = "Failed request: api_key=ak_live_998877665544332211 rejected";
        String sanitized3 = NotificationSanitizer.sanitizeErrorMessage(errorWithApiKey);
        assertThat(sanitized3).doesNotContain("ak_live_998877665544332211");
        assertThat(sanitized3).contains("api_key=[REDACTED]");
    }

    @Test
    @DisplayName("Should truncate excessively long error messages to 500 characters")
    void shouldTruncateExcessivelyLongErrors() {
        String hugeString = "Error detail: " + "A".repeat(1000);
        String sanitized = NotificationSanitizer.sanitizeErrorMessage(hugeString);

        assertThat(sanitized.length()).isLessThanOrEqualTo(500);
        assertThat(sanitized).endsWith("...");
    }

    @Test
    @DisplayName("Should handle null and empty messages gracefully")
    void shouldHandleNullOrEmpty() {
        assertThat(NotificationSanitizer.sanitizeErrorMessage((String) null)).isEqualTo("Unknown error occurred");
        assertThat(NotificationSanitizer.sanitizeErrorMessage("   ")).isEqualTo("Unknown error occurred");
    }
}
