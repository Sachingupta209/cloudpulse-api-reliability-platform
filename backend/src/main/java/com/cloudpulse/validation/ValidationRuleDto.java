package com.cloudpulse.validation;

import jakarta.validation.constraints.NotNull;

public class ValidationRuleDto {

    private Long id;

    @NotNull(message = "Rule type is required")
    private RuleType ruleType;

    private String targetExpression;
    private String expectedValue;
    private String errorMessage;
    private boolean enabled = true;

    public ValidationRuleDto() {
    }

    public ValidationRuleDto(Long id, RuleType ruleType, String targetExpression, String expectedValue, String errorMessage, boolean enabled) {
        this.id = id;
        this.ruleType = ruleType;
        this.targetExpression = targetExpression;
        this.expectedValue = expectedValue;
        this.errorMessage = errorMessage;
        this.enabled = enabled;
    }

    public static ValidationRuleDto from(ValidationRule rule) {
        return new ValidationRuleDto(
                rule.getId(),
                rule.getRuleType(),
                rule.getTargetExpression(),
                rule.getExpectedValue(),
                rule.getErrorMessage(),
                rule.isEnabled()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public RuleType getRuleType() {
        return ruleType;
    }

    public void setRuleType(RuleType ruleType) {
        this.ruleType = ruleType;
    }

    public String getTargetExpression() {
        return targetExpression;
    }

    public void setTargetExpression(String targetExpression) {
        this.targetExpression = targetExpression;
    }

    public String getExpectedValue() {
        return expectedValue;
    }

    public void setExpectedValue(String expectedValue) {
        this.expectedValue = expectedValue;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
