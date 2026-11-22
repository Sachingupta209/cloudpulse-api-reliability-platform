package com.cloudpulse.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.PathNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class ValidationEngine {

    private static final Logger log = LoggerFactory.getLogger(ValidationEngine.class);
    private final ObjectMapper objectMapper;
    private final Configuration jsonPathConfig;

    public ValidationEngine(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.jsonPathConfig = Configuration.builder()
                .options(Option.SUPPRESS_EXCEPTIONS)
                .build();
    }

    public List<ValidationResult> validate(
            int statusCode,
            long responseTimeMs,
            Map<String, List<String>> headers,
            String responseBody,
            List<ValidationRule> rules) {

        List<ValidationResult> results = new ArrayList<>();

        if (rules == null || rules.isEmpty()) {
            return results;
        }

        for (ValidationRule rule : rules) {
            if (!rule.isEnabled()) {
                continue;
            }

            try {
                ValidationResult result = evaluateRule(rule, statusCode, responseTimeMs, headers, responseBody);
                results.add(result);
            } catch (Exception ex) {
                log.warn("Error evaluating validation rule {}: {}", rule.getRuleType(), ex.getMessage());
                String customError = rule.getErrorMessage() != null ? rule.getErrorMessage() : "Rule evaluation failed: " + ex.getMessage();
                results.add(ValidationResult.failure(
                        rule.getRuleType().name(),
                        rule.getTargetExpression(),
                        rule.getExpectedValue(),
                        "ERROR",
                        customError
                ));
            }
        }

        return results;
    }

    private ValidationResult evaluateRule(
            ValidationRule rule,
            int statusCode,
            long responseTimeMs,
            Map<String, List<String>> headers,
            String responseBody) {

        switch (rule.getRuleType()) {
            case HTTP_STATUS:
                return evaluateHttpStatus(rule, statusCode);
            case RESPONSE_TIME:
                return evaluateResponseTime(rule, responseTimeMs);
            case JSON_SYNTAX:
                return evaluateJsonSyntax(rule, responseBody);
            case JSON_PATH_EXISTS:
                return evaluateJsonPathExists(rule, responseBody);
            case JSON_PATH_EQUALS:
                return evaluateJsonPathEquals(rule, responseBody);
            case JSON_PATH_NOT_EQUALS:
                return evaluateJsonPathNotEquals(rule, responseBody);
            case JSON_PATH_CONTAINS:
                return evaluateJsonPathContains(rule, responseBody);
            case HEADER_EQUALS:
                return evaluateHeaderEquals(rule, headers);
            default:
                return ValidationResult.failure(
                        rule.getRuleType().name(),
                        rule.getTargetExpression(),
                        rule.getExpectedValue(),
                        "UNKNOWN",
                        "Unsupported validation rule type: " + rule.getRuleType()
                );
        }
    }

    private ValidationResult evaluateHttpStatus(ValidationRule rule, int statusCode) {
        int expected = Integer.parseInt(rule.getExpectedValue().trim());
        if (statusCode == expected) {
            return ValidationResult.success(rule.getRuleType().name(), "status", String.valueOf(statusCode));
        }
        String errorMsg = rule.getErrorMessage() != null ? rule.getErrorMessage() :
                String.format("Expected HTTP status %d but received %d", expected, statusCode);
        return ValidationResult.failure(rule.getRuleType().name(), "status", String.valueOf(expected), String.valueOf(statusCode), errorMsg);
    }

    private ValidationResult evaluateResponseTime(ValidationRule rule, long responseTimeMs) {
        long maxAllowed = Long.parseLong(rule.getExpectedValue().trim());
        if (responseTimeMs <= maxAllowed) {
            return ValidationResult.success(rule.getRuleType().name(), "responseTimeMs", responseTimeMs + "ms");
        }
        String errorMsg = rule.getErrorMessage() != null ? rule.getErrorMessage() :
                String.format("Response time %dms exceeded maximum threshold of %dms", responseTimeMs, maxAllowed);
        return ValidationResult.failure(rule.getRuleType().name(), "responseTimeMs", maxAllowed + "ms", responseTimeMs + "ms", errorMsg);
    }

    private ValidationResult evaluateJsonSyntax(ValidationRule rule, String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return ValidationResult.failure(rule.getRuleType().name(), "body", "valid_json", "empty_body", "Response body is empty, expected valid JSON");
        }
        try {
            objectMapper.readTree(responseBody);
            return ValidationResult.success(rule.getRuleType().name(), "body", "valid_json");
        } catch (Exception ex) {
            String errorMsg = rule.getErrorMessage() != null ? rule.getErrorMessage() : "Invalid JSON response payload: " + ex.getMessage();
            return ValidationResult.failure(rule.getRuleType().name(), "body", "valid_json", "malformed_json", errorMsg);
        }
    }

    private ValidationResult evaluateJsonPathExists(ValidationRule rule, String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return ValidationResult.failure(rule.getRuleType().name(), rule.getTargetExpression(), "present", "null", "Cannot evaluate JSONPath on empty response body");
        }
        try {
            Object value = JsonPath.parse(responseBody, jsonPathConfig).read(rule.getTargetExpression());
            if (value != null) {
                return ValidationResult.success(rule.getRuleType().name(), rule.getTargetExpression(), String.valueOf(value));
            }
            String errorMsg = rule.getErrorMessage() != null ? rule.getErrorMessage() :
                    String.format("JSONPath expression '%s' returned null or was not found", rule.getTargetExpression());
            return ValidationResult.failure(rule.getRuleType().name(), rule.getTargetExpression(), "non-null", "null", errorMsg);
        } catch (PathNotFoundException ex) {
            String errorMsg = rule.getErrorMessage() != null ? rule.getErrorMessage() :
                    String.format("JSONPath expression '%s' was not found in response", rule.getTargetExpression());
            return ValidationResult.failure(rule.getRuleType().name(), rule.getTargetExpression(), "present", "missing", errorMsg);
        }
    }

    private ValidationResult evaluateJsonPathEquals(ValidationRule rule, String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return ValidationResult.failure(rule.getRuleType().name(), rule.getTargetExpression(), rule.getExpectedValue(), "null", "Response body is empty");
        }
        try {
            Object value = JsonPath.parse(responseBody, jsonPathConfig).read(rule.getTargetExpression());
            if (value == null) {
                return ValidationResult.failure(
                        rule.getRuleType().name(),
                        rule.getTargetExpression(),
                        rule.getExpectedValue(),
                        "null",
                        rule.getErrorMessage() != null ? rule.getErrorMessage() : String.format("Field at '%s' is missing or null", rule.getTargetExpression())
                );
            }
            String actualStr = String.valueOf(value).trim();
            String expectedStr = rule.getExpectedValue() != null ? rule.getExpectedValue().trim() : "";

            if (Objects.equals(actualStr, expectedStr)) {
                return ValidationResult.success(rule.getRuleType().name(), rule.getTargetExpression(), actualStr);
            }

            String errorMsg = rule.getErrorMessage() != null ? rule.getErrorMessage() :
                    String.format("JSONPath '%s' expected '%s' but received '%s'", rule.getTargetExpression(), expectedStr, actualStr);
            return ValidationResult.failure(rule.getRuleType().name(), rule.getTargetExpression(), expectedStr, actualStr, errorMsg);
        } catch (Exception ex) {
            return ValidationResult.failure(rule.getRuleType().name(), rule.getTargetExpression(), rule.getExpectedValue(), "error", ex.getMessage());
        }
    }

    private ValidationResult evaluateJsonPathNotEquals(ValidationRule rule, String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return ValidationResult.success(rule.getRuleType().name(), rule.getTargetExpression(), "empty");
        }
        try {
            Object value = JsonPath.parse(responseBody, jsonPathConfig).read(rule.getTargetExpression());
            String actualStr = value != null ? String.valueOf(value).trim() : "null";
            String expectedStr = rule.getExpectedValue() != null ? rule.getExpectedValue().trim() : "";

            if (!Objects.equals(actualStr, expectedStr)) {
                return ValidationResult.success(rule.getRuleType().name(), rule.getTargetExpression(), actualStr);
            }

            String errorMsg = rule.getErrorMessage() != null ? rule.getErrorMessage() :
                    String.format("JSONPath '%s' must not equal '%s'", rule.getTargetExpression(), expectedStr);
            return ValidationResult.failure(rule.getRuleType().name(), rule.getTargetExpression(), "not " + expectedStr, actualStr, errorMsg);
        } catch (Exception ex) {
            return ValidationResult.success(rule.getRuleType().name(), rule.getTargetExpression(), "not_found");
        }
    }

    private ValidationResult evaluateJsonPathContains(ValidationRule rule, String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return ValidationResult.failure(rule.getRuleType().name(), rule.getTargetExpression(), rule.getExpectedValue(), "null", "Response body is empty");
        }
        try {
            Object value = JsonPath.parse(responseBody, jsonPathConfig).read(rule.getTargetExpression());
            if (value == null) {
                return ValidationResult.failure(rule.getRuleType().name(), rule.getTargetExpression(), rule.getExpectedValue(), "null", "Target path evaluated to null");
            }
            String actualStr = String.valueOf(value);
            String substring = rule.getExpectedValue() != null ? rule.getExpectedValue() : "";

            if (actualStr.contains(substring)) {
                return ValidationResult.success(rule.getRuleType().name(), rule.getTargetExpression(), actualStr);
            }

            String errorMsg = rule.getErrorMessage() != null ? rule.getErrorMessage() :
                    String.format("Value at '%s' ('%s') does not contain '%s'", rule.getTargetExpression(), actualStr, substring);
            return ValidationResult.failure(rule.getRuleType().name(), rule.getTargetExpression(), substring, actualStr, errorMsg);
        } catch (Exception ex) {
            return ValidationResult.failure(rule.getRuleType().name(), rule.getTargetExpression(), rule.getExpectedValue(), "error", ex.getMessage());
        }
    }

    private ValidationResult evaluateHeaderEquals(ValidationRule rule, Map<String, List<String>> headers) {
        if (headers == null || rule.getTargetExpression() == null) {
            return ValidationResult.failure(rule.getRuleType().name(), rule.getTargetExpression(), rule.getExpectedValue(), "null", "Headers missing");
        }
        String headerName = rule.getTargetExpression();
        List<String> values = headers.get(headerName);
        if (values == null || values.isEmpty()) {
            // Case-insensitive fallback
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(headerName)) {
                    values = entry.getValue();
                    break;
                }
            }
        }

        if (values == null || values.isEmpty()) {
            String errorMsg = rule.getErrorMessage() != null ? rule.getErrorMessage() : "Header '" + headerName + "' not found in response";
            return ValidationResult.failure(rule.getRuleType().name(), headerName, rule.getExpectedValue(), "missing", errorMsg);
        }

        String actualValue = values.getFirst();
        if (Objects.equals(actualValue, rule.getExpectedValue())) {
            return ValidationResult.success(rule.getRuleType().name(), headerName, actualValue);
        }

        String errorMsg = rule.getErrorMessage() != null ? rule.getErrorMessage() :
                String.format("Header '%s' expected '%s' but received '%s'", headerName, rule.getExpectedValue(), actualValue);
        return ValidationResult.failure(rule.getRuleType().name(), headerName, rule.getExpectedValue(), actualValue, errorMsg);
    }
}
