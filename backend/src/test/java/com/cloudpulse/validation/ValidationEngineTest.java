package com.cloudpulse.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationEngineTest {

    private ValidationEngine engine;

    @BeforeEach
    void setUp() {
        engine = new ValidationEngine(new ObjectMapper());
    }

    @Test
    @DisplayName("Application-Level Failure: HTTP 200 with error body should be flagged UNHEALTHY")
    void shouldDetectApplicationLevelFailureWhenHttp200ReturnsErrorBody() {
        String payload = """
                {
                  "status": "error",
                  "message": "Database connection failed"
                }
                """;

        ValidationRule rule = new ValidationRule(
                null,
                RuleType.JSON_PATH_EQUALS,
                "$.status",
                "success",
                "Application returned error status",
                true
        );

        List<ValidationResult> results = engine.validate(200, 45, Collections.emptyMap(), payload, List.of(rule));

        assertThat(results).hasSize(1);
        ValidationResult res = results.getFirst();
        assertThat(res.isPassed()).isFalse();
        assertThat(res.getExpectedValue()).isEqualTo("success");
        assertThat(res.getActualValue()).isEqualTo("error");
        assertThat(res.getFailureReason()).contains("Application returned error status");
    }

    @Test
    @DisplayName("Application-Level Success: HTTP 200 with matching expected JSON path value")
    void shouldPassWhenExpectedJsonPathMatches() {
        String payload = """
                {
                  "status": "success",
                  "data": {
                    "health": "UP",
                    "version": "1.0.0"
                  }
                }
                """;

        ValidationRule ruleStatus = new ValidationRule(
                null,
                RuleType.JSON_PATH_EQUALS,
                "$.status",
                "success",
                null,
                true
        );

        ValidationRule ruleHealth = new ValidationRule(
                null,
                RuleType.JSON_PATH_EQUALS,
                "$.data.health",
                "UP",
                null,
                true
        );

        List<ValidationResult> results = engine.validate(200, 80, Collections.emptyMap(), payload, List.of(ruleStatus, ruleHealth));

        assertThat(results).hasSize(2);
        assertThat(results.get(0).isPassed()).isTrue();
        assertThat(results.get(1).isPassed()).isTrue();
    }

    @Test
    @DisplayName("Should detect missing expected JSON field")
    void shouldDetectMissingField() {
        String payload = """
                {
                  "status": "ok"
                }
                """;

        ValidationRule rule = new ValidationRule(
                null,
                RuleType.JSON_PATH_EXISTS,
                "$.data.user.id",
                null,
                "Required user ID is missing",
                true
        );

        List<ValidationResult> results = engine.validate(200, 30, Collections.emptyMap(), payload, List.of(rule));

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().isPassed()).isFalse();
        assertThat(results.getFirst().getFailureReason()).contains("Required user ID is missing");
    }

    @Test
    @DisplayName("Should enforce response time threshold")
    void shouldEnforceResponseTimeThreshold() {
        ValidationRule rule = new ValidationRule(
                null,
                RuleType.RESPONSE_TIME,
                null,
                "500",
                "Endpoint latency exceeded SLA",
                true
        );

        // 650ms > 500ms -> should fail
        List<ValidationResult> slowResults = engine.validate(200, 650, Collections.emptyMap(), "{}", List.of(rule));
        assertThat(slowResults.getFirst().isPassed()).isFalse();
        assertThat(slowResults.getFirst().getFailureReason()).contains("Endpoint latency exceeded SLA");

        // 350ms <= 500ms -> should pass
        List<ValidationResult> fastResults = engine.validate(200, 350, Collections.emptyMap(), "{}", List.of(rule));
        assertThat(fastResults.getFirst().isPassed()).isTrue();
    }

    @Test
    @DisplayName("Should validate JSON syntax")
    void shouldValidateJsonSyntax() {
        ValidationRule rule = new ValidationRule(
                null,
                RuleType.JSON_SYNTAX,
                null,
                null,
                null,
                true
        );

        // Invalid JSON syntax
        List<ValidationResult> badResults = engine.validate(200, 20, Collections.emptyMap(), "<html>Not JSON</html>", List.of(rule));
        assertThat(badResults.getFirst().isPassed()).isFalse();

        // Valid JSON syntax
        List<ValidationResult> goodResults = engine.validate(200, 20, Collections.emptyMap(), "{\"valid\": true}", List.of(rule));
        assertThat(goodResults.getFirst().isPassed()).isTrue();
    }

    @Test
    @DisplayName("Should validate response headers")
    void shouldValidateResponseHeaders() {
        ValidationRule rule = new ValidationRule(
                null,
                RuleType.HEADER_EQUALS,
                "Content-Type",
                "application/json",
                null,
                true
        );

        Map<String, List<String>> headers = Map.of("Content-Type", List.of("application/json"));
        List<ValidationResult> results = engine.validate(200, 20, headers, "{}", List.of(rule));
        assertThat(results.getFirst().isPassed()).isTrue();

        Map<String, List<String>> wrongHeaders = Map.of("Content-Type", List.of("text/html"));
        List<ValidationResult> badHeaders = engine.validate(200, 20, wrongHeaders, "{}", List.of(rule));
        assertThat(badHeaders.getFirst().isPassed()).isFalse();
    }
}
