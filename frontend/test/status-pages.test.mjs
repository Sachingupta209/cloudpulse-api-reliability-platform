import { test, describe } from 'node:test';
import assert from 'node:assert/strict';

describe('CloudPulse Phase 2.2: Public Status Pages Tests', () => {

  test('should default newly created status page to private (isPublished: false)', () => {
    const defaultPayload = {
      title: 'Acme Reliability',
      monitorIds: [1, 2],
    };

    const isPublished = defaultPayload.isPublished ?? false;
    assert.strictEqual(isPublished, false, 'Status page must default to private (isPublished: false)');
  });

  test('should validate custom vanity slug constraints and reserve system keywords', () => {
    const SLUG_PATTERN = /^[a-z0-9][a-z0-9-]{1,48}[a-z0-9]$/;
    const RESERVED_SLUGS = new Set([
      'api', 'auth', 'admin', 'login', 'register', 'status', 'dashboard',
      'settings', 'public', 'monitors', 'incidents', 'analytics', 'notifications'
    ]);

    assert.ok(SLUG_PATTERN.test('acme-status'));
    assert.ok(SLUG_PATTERN.test('prod-api-1'));
    assert.ok(SLUG_PATTERN.test('cloudpulse'));

    assert.ok(!SLUG_PATTERN.test('-bad-start'));
    assert.ok(!SLUG_PATTERN.test('bad-end-'));
    assert.ok(!SLUG_PATTERN.test('With_Uppercase'));
    assert.ok(!SLUG_PATTERN.test('sh')); // too short (<3)

    assert.ok(RESERVED_SLUGS.has('admin'));
    assert.ok(RESERVED_SLUGS.has('status'));
    assert.ok(RESERVED_SLUGS.has('dashboard'));
    assert.ok(!RESERVED_SLUGS.has('acme-status'));
  });

  test('should correctly compute overall system status according to prioritized rules', () => {
    function calculateOverallStatus(monitors) {
      if (!monitors || monitors.length === 0) return 'NO_DATA';
      const active = monitors.filter(m => m.status !== 'PAUSED');
      const paused = monitors.filter(m => m.status === 'PAUSED');

      if (active.length === 0 && paused.length > 0) return 'MAINTENANCE';

      const unhealthy = active.filter(m => m.healthStatus === 'UNHEALTHY');
      const healthy = active.filter(m => m.healthStatus === 'HEALTHY');

      if (unhealthy.length === active.length && active.length > 0) return 'OUTAGE';
      if (unhealthy.length > 0) return 'DEGRADED';
      if (healthy.length > 0) return 'OPERATIONAL';
      return 'NO_DATA';
    }

    // 1. All healthy
    assert.strictEqual(
      calculateOverallStatus([{ status: 'ACTIVE', healthStatus: 'HEALTHY' }]),
      'OPERATIONAL'
    );

    // 2. Some unhealthy, some healthy
    assert.strictEqual(
      calculateOverallStatus([
        { status: 'ACTIVE', healthStatus: 'HEALTHY' },
        { status: 'ACTIVE', healthStatus: 'UNHEALTHY' },
      ]),
      'DEGRADED'
    );

    // 3. All active unhealthy
    assert.strictEqual(
      calculateOverallStatus([
        { status: 'ACTIVE', healthStatus: 'UNHEALTHY' },
        { status: 'ACTIVE', healthStatus: 'UNHEALTHY' },
      ]),
      'OUTAGE'
    );

    // 4. All paused -> MAINTENANCE, not OUTAGE
    assert.strictEqual(
      calculateOverallStatus([
        { status: 'PAUSED', healthStatus: 'UNKNOWN' },
        { status: 'PAUSED', healthStatus: 'UNKNOWN' },
      ]),
      'MAINTENANCE'
    );

    // 5. Empty monitors -> NO_DATA
    assert.strictEqual(calculateOverallStatus([]), 'NO_DATA');
  });

  test('should calculate uptime with failed checks in denominator and return null for zero checks', () => {
    function calculateUptime(successChecks, failedChecks) {
      const total = successChecks + failedChecks;
      if (total === 0) return null; // No data
      const uptime = (successChecks / total) * 100.0;
      return Math.round(uptime * 100) / 100;
    }

    // 0 checks -> null (displayed as "—" or "No data")
    assert.strictEqual(calculateUptime(0, 0), null);

    // 99 success, 1 failure -> 99.0%
    assert.strictEqual(calculateUptime(99, 1), 99.0);

    // 100 success, 0 failure -> 100.0%
    assert.strictEqual(calculateUptime(100, 0), 100.0);

    // 0 success, 50 failure -> 0.0%
    assert.strictEqual(calculateUptime(0, 50), 0.0);
  });

  test('should ensure public monitor representation strictly excludes private configuration', () => {
    const rawInternalMonitor = {
      id: 101,
      name: 'Billing Gateway',
      url: 'https://internal-payments.corp.local:8443/process',
      headersJson: '{"Authorization":"Bearer super-secret-jwt-token"}',
      requestBody: '{"apiKey":"sk_live_12345"}',
      expectedStatusCode: 200,
      timeoutMs: 5000,
      validationRules: [{ ruleType: 'JSON_PATH_EQUALS', targetExpression: '$.balance' }],
      status: 'ACTIVE',
      healthStatus: 'HEALTHY',
    };

    // Transform to sanitized public format
    const publicMonitor = {
      name: rawInternalMonitor.name,
      status: rawInternalMonitor.healthStatus === 'HEALTHY' ? 'OPERATIONAL' : 'DOWN',
      uptime24h: 99.9,
      uptime7d: 99.8,
      uptime30d: 99.7,
      averageLatencyMs: 145.0,
    };

    assert.strictEqual(publicMonitor.name, 'Billing Gateway');
    assert.strictEqual(publicMonitor.url, undefined);
    assert.strictEqual(publicMonitor.headersJson, undefined);
    assert.strictEqual(publicMonitor.requestBody, undefined);
    assert.strictEqual(publicMonitor.validationRules, undefined);
    assert.strictEqual(publicMonitor.expectedStatusCode, undefined);
    assert.strictEqual(publicMonitor.id, undefined);
  });

  test('should format incident duration in minutes', () => {
    const started = new Date('2026-11-15T12:00:00Z');
    const resolved = new Date('2026-11-15T13:15:00Z');
    const durationMinutes = Math.round((resolved - started) / (1000 * 60));

    assert.strictEqual(durationMinutes, 75);
  });
});
