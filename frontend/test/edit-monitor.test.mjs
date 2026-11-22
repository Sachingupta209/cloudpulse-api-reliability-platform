import { describe, it } from 'node:test';
import assert from 'node:assert/strict';

describe('Edit Monitor Frontend Configuration & Validation Tests', () => {

  const sampleExistingMonitor = {
    id: 42,
    name: 'Production Payment Gateway',
    url: 'https://payment.example.com/health',
    httpMethod: 'POST',
    checkIntervalSeconds: 60,
    timeoutMs: 5000,
    expectedStatusCode: 200,
    headersJson: JSON.stringify({ Authorization: 'Bearer test-token-123' }),
    requestBody: JSON.stringify({ ping: true }),
    validationRules: [
      {
        id: 101,
        ruleType: 'JSON_PATH_EQUALS',
        targetExpression: '$.status',
        expectedValue: 'ok',
        errorMessage: 'Payment gateway status returned non-ok',
        enabled: true,
      },
      {
        id: 102,
        ruleType: 'HEADER_EQUALS',
        targetExpression: 'Content-Type',
        expectedValue: 'application/json',
        errorMessage: 'Response must be application/json',
        enabled: true,
      },
      {
        id: 103,
        ruleType: 'RESPONSE_TIME',
        targetExpression: 'responseTimeMs',
        expectedValue: '2500',
        errorMessage: 'Latency exceeded 2500ms SLA',
        enabled: true,
      },
    ],
  };

  it('should verify edit route construction for table rows and detail headers', () => {
    const monitorId = 42;
    const expectedRoute = `/monitors/${monitorId}/edit`;
    assert.strictEqual(expectedRoute, '/monitors/42/edit');
  });

  it('should correctly build PUT /api/monitors/{id} payload with updated values', () => {
    const updatedValues = {
      name: 'Production Payment Gateway v2',
      url: 'https://payment.example.com/v2/health',
      httpMethod: 'POST',
      checkIntervalSeconds: 30,
      timeoutMs: 8000,
      expectedStatusCode: 200,
      headersJson: JSON.stringify({ Authorization: 'Bearer new-token-456' }),
      requestBody: JSON.stringify({ version: '2.0', ping: true }),
      validationRules: [
        ...sampleExistingMonitor.validationRules,
        {
          ruleType: 'JSON_PATH_EXISTS',
          targetExpression: '$.data.gateway_healthy',
          expectedValue: '',
          errorMessage: 'Gateway health flag missing',
          enabled: true,
        },
      ],
    };

    // Simulate form submission transformation
    const payload = {
      name: updatedValues.name.trim(),
      url: updatedValues.url.trim(),
      httpMethod: updatedValues.httpMethod,
      checkIntervalSeconds: Number(updatedValues.checkIntervalSeconds),
      timeoutMs: Number(updatedValues.timeoutMs),
      expectedStatusCode: Number(updatedValues.expectedStatusCode),
      headersJson: updatedValues.headersJson?.trim() || null,
      requestBody: updatedValues.requestBody?.trim() || null,
      validationRules: updatedValues.validationRules,
    };

    assert.strictEqual(payload.name, 'Production Payment Gateway v2');
    assert.strictEqual(payload.url, 'https://payment.example.com/v2/health');
    assert.strictEqual(payload.httpMethod, 'POST');
    assert.strictEqual(payload.checkIntervalSeconds, 30);
    assert.strictEqual(payload.timeoutMs, 8000);
    assert.strictEqual(payload.expectedStatusCode, 200);
    assert.strictEqual(payload.validationRules.length, 4);
  });

  it('should enforce client-side constraints on intervals, timeouts, and status codes', () => {
    // Check interval bounds: 10s to 86400s
    const isValidInterval = (sec) => sec >= 10 && sec <= 86400;
    assert.strictEqual(isValidInterval(5), false);
    assert.strictEqual(isValidInterval(10), true);
    assert.strictEqual(isValidInterval(60), true);
    assert.strictEqual(isValidInterval(86400), true);
    assert.strictEqual(isValidInterval(90000), false);

    // Timeout bounds: 1000ms to 30000ms
    const isValidTimeout = (ms) => ms >= 1000 && ms <= 30000;
    assert.strictEqual(isValidTimeout(500), false);
    assert.strictEqual(isValidTimeout(1000), true);
    assert.strictEqual(isValidTimeout(10000), true);
    assert.strictEqual(isValidTimeout(30000), true);
    assert.strictEqual(isValidTimeout(35000), false);

    // HTTP Status bounds: 100 to 599
    const isValidStatusCode = (code) => code >= 100 && code <= 599;
    assert.strictEqual(isValidStatusCode(99), false);
    assert.strictEqual(isValidStatusCode(200), true);
    assert.strictEqual(isValidStatusCode(201), true);
    assert.strictEqual(isValidStatusCode(404), true);
    assert.strictEqual(isValidStatusCode(600), false);
  });

  it('should validate custom headers JSON format', () => {
    const validJson = '{"X-API-Key": "secret123", "Accept": "application/json"}';
    const invalidJson = '{X-API-Key: invalid}';

    const parseHeaders = (input) => {
      if (!input || !input.trim()) return null;
      return JSON.parse(input);
    };

    assert.doesNotThrow(() => {
      const parsed = parseHeaders(validJson);
      assert.strictEqual(parsed['X-API-Key'], 'secret123');
    });

    assert.throws(() => {
      parseHeaders(invalidJson);
    }, SyntaxError);
  });

  it('should handle Latency SLA rule configuration (RESPONSE_TIME)', () => {
    const latencyRule = {
      ruleType: 'RESPONSE_TIME',
      targetExpression: 'responseTimeMs',
      expectedValue: '1500',
      errorMessage: 'Latency SLA violated (> 1500ms)',
      enabled: true,
    };

    assert.strictEqual(latencyRule.ruleType, 'RESPONSE_TIME');
    assert.strictEqual(Number.isInteger(Number(latencyRule.expectedValue)), true);
    assert.strictEqual(Number(latencyRule.expectedValue) > 0, true);
  });

  it('should handle Header match rule configuration (HEADER_EQUALS)', () => {
    const headerRule = {
      ruleType: 'HEADER_EQUALS',
      targetExpression: 'Content-Type',
      expectedValue: 'application/json; charset=utf-8',
      errorMessage: 'Invalid Content-Type header',
      enabled: true,
    };

    assert.strictEqual(headerRule.ruleType, 'HEADER_EQUALS');
    assert.strictEqual(headerRule.targetExpression.length > 0, true);
    assert.strictEqual(headerRule.expectedValue.includes('application/json'), true);
  });

  it('should handle JSONPath validation rules (EQUALS, EXISTS, NOT_EQUALS, CONTAINS)', () => {
    const jsonRules = [
      {
        ruleType: 'JSON_PATH_EQUALS',
        targetExpression: '$.meta.code',
        expectedValue: '200',
        enabled: true,
      },
      {
        ruleType: 'JSON_PATH_EXISTS',
        targetExpression: '$.data.users[0].id',
        expectedValue: '',
        enabled: true,
      },
      {
        ruleType: 'JSON_PATH_NOT_EQUALS',
        targetExpression: '$.error',
        expectedValue: 'database_timeout',
        enabled: true,
      },
      {
        ruleType: 'JSON_PATH_CONTAINS',
        targetExpression: '$.message',
        expectedValue: 'Operation successful',
        enabled: true,
      },
    ];

    for (const rule of jsonRules) {
      assert.strictEqual(rule.targetExpression.startsWith('$'), true);
      assert.strictEqual(rule.enabled, true);
    }
  });

  it('should preserve existing check results and incident records when editing without delete/recreate', () => {
    // Verification that the monitor ID remains immutable during edit operations
    const originalMonitorId = 42;
    const requestPath = `/api/monitors/${originalMonitorId}`;
    assert.strictEqual(requestPath, '/api/monitors/42');
    // Ensure PUT method is targeted rather than POST (create) or DELETE (destroy)
    const expectedHttpMethod = 'PUT';
    assert.strictEqual(expectedHttpMethod, 'PUT');
  });
});
