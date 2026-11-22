package com.cloudpulse.validation;

public class ValidationResult {

    private final boolean passed;
    private final String ruleType;
    private final String expression;
    private final String expectedValue;
    private final String actualValue;
    private final String failureReason;

    public ValidationResult(boolean passed, String ruleType, String expression, String expectedValue, String actualValue, String failureReason) {
        this.passed = passed;
        this.ruleType = ruleType;
        this.expression = expression;
        this.expectedValue = expectedValue;
        this.actualValue = actualValue;
        this.failureReason = failureReason;
    }

    public static ValidationResult success(String ruleType, String expression, String value) {
        return new ValidationResult(true, ruleType, expression, value, value, null);
    }

    public static ValidationResult failure(String ruleType, String expression, String expectedValue, String actualValue, String failureReason) {
        return new ValidationResult(false, ruleType, expression, expectedValue, actualValue, failureReason);
    }

    public boolean isPassed() {
        return passed;
    }

    public String getRuleType() {
        return ruleType;
    }

    public String getExpression() {
        return expression;
    }

    public String getExpectedValue() {
        return expectedValue;
    }

    public String getActualValue() {
        return actualValue;
    }

    public String getFailureReason() {
        return failureReason;
    }
}
