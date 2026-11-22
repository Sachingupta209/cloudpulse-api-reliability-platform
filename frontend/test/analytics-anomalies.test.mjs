import { test, describe } from 'node:test';
import assert from 'node:assert/strict';

describe('CloudPulse Phase 2.4: Advanced Analytics & Statistical Anomaly Detection Tests', () => {

  test('should calculate Z-score accurately and flag latency spikes exceeding threshold (z >= 3.0)', () => {
    function computeZScore(value, history) {
      if (!history || history.length < 5) return null;
      const mean = history.reduce((sum, v) => sum + v, 0) / history.length;
      const variance = history.reduce((sum, v) => sum + Math.pow(v - mean, 2), 0) / history.length;
      const stdDev = Math.max(15.0, Math.sqrt(variance)); // floor std dev at 15
      return {
        mean: Math.round(mean * 10) / 10,
        stdDev: Math.round(stdDev * 10) / 10,
        zScore: (value - mean) / stdDev,
      };
    }

    const stableHistory = [40, 42, 45, 41, 43, 44, 40, 42, 45, 43];
    const normalPing = 48;
    const spikePing = 350;

    const normalResult = computeZScore(normalPing, stableHistory);
    assert.ok(normalResult.zScore < 3.0, 'Normal latency must not trigger Z-score threshold');

    const spikeResult = computeZScore(spikePing, stableHistory);
    assert.ok(spikeResult.zScore >= 3.0, 'Significant spike must exceed Z-score threshold 3.0');
    assert.ok(spikeResult.zScore > 10.0, '350ms vs 42ms baseline should have large Z-score');
  });

  test('should correctly classify anomaly severity based on statistical deviation and magnitude', () => {
    function classifySeverity(zScore, observed, baseline) {
      if (zScore >= 5.0 || observed >= 4.0 * baseline) return 'CRITICAL';
      if (zScore >= 4.0 || observed >= 3.0 * baseline) return 'HIGH';
      if (zScore >= 3.0 || observed >= 2.5 * baseline) return 'MEDIUM';
      return 'LOW';
    }

    assert.strictEqual(classifySeverity(5.5, 450, 50), 'CRITICAL');
    assert.strictEqual(classifySeverity(4.2, 160, 50), 'HIGH');
    assert.strictEqual(classifySeverity(3.1, 130, 50), 'MEDIUM');
    assert.strictEqual(classifySeverity(1.5, 60, 50), 'LOW');
  });

  test('should calculate MTTR (Mean Time to Recovery) correctly from resolved incidents', () => {
    function calculateMttr(resolvedIncidents) {
      if (!resolvedIncidents || resolvedIncidents.length === 0) return null;
      let totalMinutes = 0;
      for (const inc of resolvedIncidents) {
        const start = new Date(inc.startedAt).getTime();
        const end = new Date(inc.resolvedAt).getTime();
        const minutes = Math.max(1, Math.round((end - start) / (1000 * 60)));
        totalMinutes += minutes;
      }
      return Math.round((totalMinutes / resolvedIncidents.length) * 10) / 10;
    }

    const incidents = [
      { startedAt: '2026-11-15T10:00:00Z', resolvedAt: '2026-11-15T10:15:00Z' }, // 15 mins
      { startedAt: '2026-11-15T12:00:00Z', resolvedAt: '2026-11-15T12:45:00Z' }, // 45 mins
    ];

    const mttr = calculateMttr(incidents);
    assert.strictEqual(mttr, 30.0, 'MTTR should be the arithmetic average of incident durations: (15 + 45)/2 = 30');
    assert.strictEqual(calculateMttr([]), null);
  });

  test('should calculate MTBF (Mean Time Between Failures) correctly', () => {
    function calculateMtbf(monitorCount, incidentCount, totalDowntimeMinutes, windowDays = 30) {
      if (!incidentCount || incidentCount <= 0 || !monitorCount || monitorCount <= 0) return null;
      const totalOperatingHours = monitorCount * windowDays * 24;
      const downtimeHours = totalDowntimeMinutes / 60;
      const uptimeHours = Math.max(0, totalOperatingHours - downtimeHours);
      return Math.round((uptimeHours / incidentCount) * 10) / 10;
    }

    // 2 monitors, 2 incidents, 60 mins downtime over 30 days
    // Total operating hours = 2 * 30 * 24 = 1440h. Downtime = 1h. Uptime = 1439h. MTBF = 1439 / 2 = 719.5h
    const mtbf = calculateMtbf(2, 2, 60, 30);
    assert.strictEqual(mtbf, 719.5);
    assert.strictEqual(calculateMtbf(0, 2, 0), null);
  });

  test('should accurately filter anomalies list by category and acknowledgment state', () => {
    const anomalyList = [
      { id: 1, anomalyType: 'LATENCY_SPIKE', acknowledged: false },
      { id: 2, anomalyType: 'LATENCY_SPIKE', acknowledged: true },
      { id: 3, anomalyType: 'FAILURE_BURST', acknowledged: false },
      { id: 4, anomalyType: 'UPTIME_DEGRADATION', acknowledged: true },
    ];

    function filterAnomalies(list, filter) {
      return list.filter(a => {
        if (filter === 'UNACK') return !a.acknowledged;
        if (filter === 'LATENCY_SPIKE') return a.anomalyType === 'LATENCY_SPIKE';
        if (filter === 'FAILURE_BURST') return a.anomalyType === 'FAILURE_BURST';
        if (filter === 'UPTIME_DEGRADATION') return a.anomalyType === 'UPTIME_DEGRADATION';
        return true;
      });
    }

    assert.strictEqual(filterAnomalies(anomalyList, 'ALL').length, 4);
    assert.strictEqual(filterAnomalies(anomalyList, 'UNACK').length, 2);
    assert.strictEqual(filterAnomalies(anomalyList, 'LATENCY_SPIKE').length, 2);
    assert.strictEqual(filterAnomalies(anomalyList, 'FAILURE_BURST').length, 1);
    assert.strictEqual(filterAnomalies(anomalyList, 'UPTIME_DEGRADATION').length, 1);
  });
});
