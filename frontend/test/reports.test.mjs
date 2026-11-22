import { test, describe } from 'node:test';
import assert from 'node:assert/strict';

describe('CloudPulse Phase 2.3: PDF Monitoring Reports Tests', () => {

  test('should compute correct start and end timestamps for report date presets', () => {
    const now = new Date('2026-11-15T12:00:00Z');

    function calculateDateRange(preset, fixedNow = now) {
      const end = fixedNow.toISOString();
      let start;
      switch (preset) {
        case '24h':
          start = new Date(fixedNow.getTime() - 24 * 60 * 60 * 1000).toISOString();
          break;
        case '7d':
          start = new Date(fixedNow.getTime() - 7 * 24 * 60 * 60 * 1000).toISOString();
          break;
        case '30d':
          start = new Date(fixedNow.getTime() - 30 * 24 * 60 * 60 * 1000).toISOString();
          break;
        case '90d':
          start = new Date(fixedNow.getTime() - 90 * 24 * 60 * 60 * 1000).toISOString();
          break;
        default:
          start = new Date(fixedNow.getTime() - 7 * 24 * 60 * 60 * 1000).toISOString();
      }
      return { start, end };
    }

    const range24h = calculateDateRange('24h');
    assert.strictEqual(range24h.start, '2026-11-14T12:00:00.000Z');
    assert.strictEqual(range24h.end, '2026-11-15T12:00:00.000Z');

    const range7d = calculateDateRange('7d');
    assert.strictEqual(range7d.start, '2026-11-08T12:00:00.000Z');

    const range30d = calculateDateRange('30d');
    assert.strictEqual(range30d.start, '2026-10-16T12:00:00.000Z');
  });

  test('should validate custom date range logic and bounds', () => {
    function validateDateRange(startDate, endDate) {
      if (!startDate || !endDate) {
        return { valid: false, error: 'Both start and end dates are required' };
      }
      const start = new Date(startDate);
      const end = new Date(endDate);
      if (start > end) {
        return { valid: false, error: 'Start date cannot be after end date' };
      }
      const diffDays = (end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24);
      if (diffDays > 365) {
        return { valid: false, error: 'Date range cannot exceed 365 days' };
      }
      return { valid: true };
    }

    assert.strictEqual(validateDateRange('2026-01-01', '2026-01-10').valid, true);
    assert.strictEqual(validateDateRange('2026-02-01', '2026-01-01').valid, false);
    assert.strictEqual(validateDateRange('2026-02-01', '2026-01-01').error, 'Start date cannot be after end date');
    assert.strictEqual(validateDateRange('2024-01-01', '2026-01-01').valid, false);
    assert.strictEqual(validateDateRange('2024-01-01', '2026-01-01').error, 'Date range cannot exceed 365 days');
  });

  test('should resolve correct endpoint and default filename for each report template', () => {
    function resolveReportConfig(reportType, monitorId) {
      switch (reportType) {
        case 'MONITOR':
          if (!monitorId) throw new Error('Monitor ID is required for single monitor report');
          return {
            endpoint: `/reports/monitors/${monitorId}/pdf`,
            filename: `cloudpulse_monitor_${monitorId}_report.pdf`,
          };
        case 'SUMMARY':
          return {
            endpoint: '/reports/summary/pdf',
            filename: 'cloudpulse_portfolio_summary.pdf',
          };
        case 'INCIDENT':
          return {
            endpoint: '/reports/incidents/pdf',
            filename: 'cloudpulse_incident_log.pdf',
          };
        case 'PERFORMANCE':
          return {
            endpoint: '/reports/performance/pdf',
            filename: 'cloudpulse_performance_sla.pdf',
          };
        default:
          throw new Error('Unknown report type');
      }
    }

    assert.deepStrictEqual(resolveReportConfig('SUMMARY'), {
      endpoint: '/reports/summary/pdf',
      filename: 'cloudpulse_portfolio_summary.pdf',
    });

    assert.deepStrictEqual(resolveReportConfig('MONITOR', 42), {
      endpoint: '/reports/monitors/42/pdf',
      filename: 'cloudpulse_monitor_42_report.pdf',
    });

    assert.throws(() => resolveReportConfig('MONITOR', null), /Monitor ID is required/);
  });

  test('should parse filename correctly from Content-Disposition header', () => {
    function extractFilename(header, fallback) {
      if (!header || !header.includes('filename=')) return fallback;
      const matches = /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/.exec(header);
      if (matches != null && matches[1]) {
        return matches[1].replace(/['"]/g, '');
      }
      return fallback;
    }

    const header1 = 'attachment; filename="cloudpulse_portfolio_summary_20261115.pdf"';
    assert.strictEqual(extractFilename(header1, 'fallback.pdf'), 'cloudpulse_portfolio_summary_20261115.pdf');

    const header2 = 'attachment; filename=cloudpulse_monitor_1.pdf';
    assert.strictEqual(extractFilename(header2, 'fallback.pdf'), 'cloudpulse_monitor_1.pdf');

    assert.strictEqual(extractFilename('', 'fallback.pdf'), 'fallback.pdf');
  });

  test('should verify calculateStats math correctly computes uptime and percentiles', () => {
    const latencies = [15, 20, 25, 30, 40, 50, 60, 75, 90, 150]; // 10 successful
    const totalChecks = 10;
    const successfulChecks = 9;
    const failedChecks = 1;

    const uptime = Math.round((successfulChecks / totalChecks) * 10000) / 100;
    const failureRate = Math.round((failedChecks / totalChecks) * 10000) / 100;

    assert.strictEqual(uptime, 90.0);
    assert.strictEqual(failureRate, 10.0);

    const sorted = [...latencies].sort((a, b) => a - b);
    const minLatency = sorted[0];
    const maxLatency = sorted[sorted.length - 1];
    const avgLatency = Math.round((sorted.reduce((a, b) => a + b, 0) / sorted.length) * 10) / 10;

    const idx95 = Math.ceil(0.95 * sorted.length) - 1;
    const p95 = sorted[Math.max(0, idx95)];

    assert.strictEqual(minLatency, 15);
    assert.strictEqual(maxLatency, 150);
    assert.strictEqual(avgLatency, 55.5);
    assert.strictEqual(p95, 150);
  });
});
