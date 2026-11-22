package com.cloudpulse.monitor;

import com.cloudpulse.validation.ValidationRuleDto;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

public class MonitorRequest {

    @NotBlank(message = "Monitor name is required")
    private String name;

    @NotBlank(message = "Monitor URL is required")
    private String url;

    @NotNull(message = "HTTP Method is required")
    private HttpMethod httpMethod = HttpMethod.GET;

    @Min(value = 10, message = "Interval must be at least 10 seconds")
    @Max(value = 86400, message = "Interval cannot exceed 86400 seconds (24 hours)")
    private int checkIntervalSeconds = 60;

    @Min(value = 1000, message = "Timeout must be at least 1000 ms")
    @Max(value = 30000, message = "Timeout cannot exceed 30000 ms")
    private int timeoutMs = 10000;

    @Min(value = 100, message = "Status code must be between 100 and 599")
    @Max(value = 599, message = "Status code must be between 100 and 599")
    private Integer expectedStatusCode = 200;

    private String headersJson;
    private String requestBody;

    private List<ValidationRuleDto> validationRules = new ArrayList<>();

    public MonitorRequest() {
    }

    public MonitorRequest(String name, String url, HttpMethod httpMethod, int checkIntervalSeconds, int timeoutMs, Integer expectedStatusCode) {
        this.name = name;
        this.url = url;
        this.httpMethod = httpMethod;
        this.checkIntervalSeconds = checkIntervalSeconds;
        this.timeoutMs = timeoutMs;
        this.expectedStatusCode = expectedStatusCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(HttpMethod httpMethod) {
        this.httpMethod = httpMethod;
    }

    public int getCheckIntervalSeconds() {
        return checkIntervalSeconds;
    }

    public void setCheckIntervalSeconds(int checkIntervalSeconds) {
        this.checkIntervalSeconds = checkIntervalSeconds;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public Integer getExpectedStatusCode() {
        return expectedStatusCode;
    }

    public void setExpectedStatusCode(Integer expectedStatusCode) {
        this.expectedStatusCode = expectedStatusCode;
    }

    public String getHeadersJson() {
        return headersJson;
    }

    public void setHeadersJson(String headersJson) {
        this.headersJson = headersJson;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    public List<ValidationRuleDto> getValidationRules() {
        return validationRules;
    }

    public void setValidationRules(List<ValidationRuleDto> validationRules) {
        this.validationRules = validationRules;
    }
}
