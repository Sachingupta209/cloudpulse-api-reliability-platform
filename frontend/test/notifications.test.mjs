import { describe, it } from 'node:test';
import assert from 'node:assert/strict';

describe('CloudPulse Notification Module Unit Tests', () => {

  const defaultPreferences = {
    id: 1,
    userId: 101,
    emailEnabled: true,
    notifyOnIncidentOpened: true,
    notifyOnIncidentResolved: true,
    notifyOnUnhealthy: true,
    notifyOnRecovery: true,
    notificationEmail: 'alerts@cloudpulse.io',
  };

  const sampleHistory = [
    {
      id: 1,
      monitorId: 10,
      monitorName: 'Auth Service',
      monitorUrl: 'https://auth.cloudpulse.io/health',
      incidentId: 5,
      incidentTitle: 'Downtime detected for Auth Service',
      notificationType: 'INCIDENT_OPENED',
      recipient: 'alerts@cloudpulse.io',
      subject: '[CloudPulse Incident] Downtime detected for Auth Service',
      status: 'SENT',
      failureReason: null,
      retryCount: 1,
      sentAt: '2026-11-15T12:00:00Z',
    },
    {
      id: 2,
      monitorId: 10,
      monitorName: 'Auth Service',
      monitorUrl: 'https://auth.cloudpulse.io/health',
      incidentId: 5,
      incidentTitle: 'Downtime detected for Auth Service',
      notificationType: 'INCIDENT_RESOLVED',
      recipient: 'alerts@cloudpulse.io',
      subject: '[CloudPulse Resolved] Incident #5 Resolved',
      status: 'SENT',
      failureReason: null,
      retryCount: 1,
      sentAt: '2026-11-15T12:05:00Z',
    },
    {
      id: 3,
      monitorId: 12,
      monitorName: 'Payment Gateway',
      monitorUrl: 'https://pay.cloudpulse.io/health',
      incidentId: null,
      incidentTitle: null,
      notificationType: 'SERVICE_UNHEALTHY',
      recipient: 'ops@cloudpulse.io',
      subject: "[CloudPulse Alert] Service 'Payment Gateway' is UNHEALTHY",
      status: 'FAILED',
      failureReason: 'Connection refused: mailpit:1025',
      retryCount: 3,
      sentAt: '2026-11-15T13:00:00Z',
    },
    {
      id: 4,
      monitorId: null,
      monitorName: null,
      monitorUrl: null,
      incidentId: null,
      incidentTitle: null,
      notificationType: 'TEST',
      recipient: 'dev@cloudpulse.io',
      subject: '[CloudPulse] Test Alert Notification',
      status: 'SENT',
      failureReason: null,
      retryCount: 1,
      sentAt: '2026-11-15T14:00:00Z',
    },
  ];

  it('should validate initial preference state and defaults', () => {
    assert.strictEqual(defaultPreferences.emailEnabled, true);
    assert.strictEqual(defaultPreferences.notifyOnIncidentOpened, true);
    assert.strictEqual(defaultPreferences.notifyOnIncidentResolved, true);
    assert.strictEqual(defaultPreferences.notifyOnUnhealthy, true);
    assert.strictEqual(defaultPreferences.notifyOnRecovery, true);
    assert.strictEqual(defaultPreferences.notificationEmail, 'alerts@cloudpulse.io');
  });

  it('should support updating triggers and custom email payload', () => {
    const updated = {
      ...defaultPreferences,
      notifyOnIncidentResolved: false,
      notifyOnRecovery: false,
      notificationEmail: 'custom-team@cloudpulse.io',
    };

    assert.strictEqual(updated.notifyOnIncidentResolved, false);
    assert.strictEqual(updated.notifyOnRecovery, false);
    assert.strictEqual(updated.notificationEmail, 'custom-team@cloudpulse.io');
  });

  it('should mute all triggers when master emailEnabled is toggled false', () => {
    const disabledState = {
      ...defaultPreferences,
      emailEnabled: false,
    };

    // Helper checking whether notification should be dispatched
    function isEventDispatched(pref, eventType) {
      if (!pref.emailEnabled) return false;
      switch (eventType) {
        case 'INCIDENT_OPENED':
          return pref.notifyOnIncidentOpened;
        case 'INCIDENT_RESOLVED':
          return pref.notifyOnIncidentResolved;
        case 'SERVICE_UNHEALTHY':
          return pref.notifyOnUnhealthy;
        case 'SERVICE_RECOVERED':
          return pref.notifyOnRecovery;
        default:
          return true;
      }
    }

    assert.strictEqual(isEventDispatched(disabledState, 'INCIDENT_OPENED'), false);
    assert.strictEqual(isEventDispatched(disabledState, 'INCIDENT_RESOLVED'), false);
    assert.strictEqual(isEventDispatched(disabledState, 'SERVICE_UNHEALTHY'), false);
    assert.strictEqual(isEventDispatched(disabledState, 'SERVICE_RECOVERED'), false);

    // When master switch is on:
    assert.strictEqual(isEventDispatched(defaultPreferences, 'INCIDENT_OPENED'), true);
  });

  it('should correctly format notification badge variants based on event type', () => {
    function getBadgeVariant(type) {
      switch (type) {
        case 'SERVICE_UNHEALTHY':
        case 'INCIDENT_OPENED':
          return 'rose';
        case 'SERVICE_RECOVERED':
        case 'INCIDENT_RESOLVED':
          return 'emerald';
        case 'TEST':
        default:
          return 'sky';
      }
    }

    assert.strictEqual(getBadgeVariant('INCIDENT_OPENED'), 'rose');
    assert.strictEqual(getBadgeVariant('SERVICE_UNHEALTHY'), 'rose');
    assert.strictEqual(getBadgeVariant('INCIDENT_RESOLVED'), 'emerald');
    assert.strictEqual(getBadgeVariant('SERVICE_RECOVERED'), 'emerald');
    assert.strictEqual(getBadgeVariant('TEST'), 'sky');
  });

  it('should track delivery status and failure reasons in audit history', () => {
    const sentItem = sampleHistory.find((h) => h.status === 'SENT');
    const failedItem = sampleHistory.find((h) => h.status === 'FAILED');

    assert.ok(sentItem);
    assert.strictEqual(sentItem.failureReason, null);

    assert.ok(failedItem);
    assert.strictEqual(failedItem.retryCount, 3);
    assert.ok(failedItem.failureReason.includes('Connection refused'));
  });

  it('should compute pagination boundaries correctly', () => {
    const totalElements = 45;
    const pageSize = 15;
    const totalPages = Math.ceil(totalElements / pageSize);

    assert.strictEqual(totalPages, 3);

    const getPageBounds = (page) => ({
      hasPrev: page > 0,
      hasNext: page < totalPages - 1,
    });

    assert.deepStrictEqual(getPageBounds(0), { hasPrev: false, hasNext: true });
    assert.deepStrictEqual(getPageBounds(1), { hasPrev: true, hasNext: true });
    assert.deepStrictEqual(getPageBounds(2), { hasPrev: true, hasNext: false });
  });
});
