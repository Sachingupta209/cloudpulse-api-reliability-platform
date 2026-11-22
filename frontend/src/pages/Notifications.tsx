import React, { useEffect, useState } from 'react';
import {
  AlertCircle,
  AlertTriangle,
  Bell,
  CheckCircle2,
  Clock,
  History,
  Mail,
  RefreshCw,
  Send,
  ShieldAlert,
  Sparkles,
  Zap,
} from 'lucide-react';
import { Navbar } from '../components/layout/Navbar';
import { notificationsApi } from '../api/notifications';
import type { UpdatePreferencesRequest } from '../api/notifications';
import type { NotificationHistoryItem, NotificationPreference, NotificationType } from '../types';

export const Notifications: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'settings' | 'history'>('settings');

  // Preferences State
  const [preferences, setPreferences] = useState<NotificationPreference | null>(null);
  const [prefsLoading, setPrefsLoading] = useState(true);
  const [prefsSaving, setPrefsSaving] = useState(false);
  const [prefsSuccess, setPrefsSuccess] = useState<string | null>(null);
  const [prefsError, setPrefsError] = useState<string | null>(null);

  // Form State
  const [emailEnabled, setEmailEnabled] = useState(true);
  const [notifyOnIncidentOpened, setNotifyOnIncidentOpened] = useState(true);
  const [notifyOnIncidentResolved, setNotifyOnIncidentResolved] = useState(true);
  const [notifyOnUnhealthy, setNotifyOnUnhealthy] = useState(true);
  const [notifyOnRecovery, setNotifyOnRecovery] = useState(true);
  const [notificationEmail, setNotificationEmail] = useState('');

  // Test Notification State
  const [testSending, setTestSending] = useState(false);
  const [testResult, setTestResult] = useState<{ success: boolean; message: string } | null>(null);

  // History State
  const [history, setHistory] = useState<NotificationHistoryItem[]>([]);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [historyPage, setHistoryPage] = useState(0);
  const [historyTotalPages, setHistoryTotalPages] = useState(0);
  const [historyTotalElements, setHistoryTotalElements] = useState(0);
  const [historyError, setHistoryError] = useState<string | null>(null);

  useEffect(() => {
    fetchPreferences();
  }, []);

  useEffect(() => {
    if (activeTab === 'history') {
      fetchHistory(historyPage);
    }
  }, [activeTab, historyPage]);

  const fetchPreferences = async () => {
    setPrefsLoading(true);
    setPrefsError(null);
    try {
      const res = await notificationsApi.getPreferences();
      if (res.data) {
        setPreferences(res.data);
        setEmailEnabled(res.data.emailEnabled);
        setNotifyOnIncidentOpened(res.data.notifyOnIncidentOpened);
        setNotifyOnIncidentResolved(res.data.notifyOnIncidentResolved);
        setNotifyOnUnhealthy(res.data.notifyOnUnhealthy);
        setNotifyOnRecovery(res.data.notifyOnRecovery);
        setNotificationEmail(res.data.notificationEmail || '');
      }
    } catch (err: any) {
      setPrefsError(err.response?.data?.message || 'Failed to load notification preferences');
    } finally {
      setPrefsLoading(false);
    }
  };

  const fetchHistory = async (page: number) => {
    setHistoryLoading(true);
    setHistoryError(null);
    try {
      const res = await notificationsApi.getHistory(page, 15);
      if (res.data) {
        setHistory(res.data.content || []);
        setHistoryTotalPages(res.data.totalPages || 0);
        setHistoryTotalElements(res.data.totalElements || 0);
      }
    } catch (err: any) {
      setHistoryError(err.response?.data?.message || 'Failed to load notification history');
    } finally {
      setHistoryLoading(false);
    }
  };

  const handleSavePreferences = async (e: React.FormEvent) => {
    e.preventDefault();
    setPrefsSaving(true);
    setPrefsSuccess(null);
    setPrefsError(null);

    const payload: UpdatePreferencesRequest = {
      emailEnabled,
      notifyOnIncidentOpened,
      notifyOnIncidentResolved,
      notifyOnUnhealthy,
      notifyOnRecovery,
      notificationEmail: notificationEmail.trim() || undefined,
    };

    try {
      const res = await notificationsApi.updatePreferences(payload);
      if (res.data) {
        setPreferences(res.data);
        setPrefsSuccess('Notification preferences saved and activated successfully.');
      }
    } catch (err: any) {
      setPrefsError(err.response?.data?.message || 'Failed to update notification preferences');
    } finally {
      setPrefsSaving(false);
    }
  };

  const handleSendTestNotification = async () => {
    setTestSending(true);
    setTestResult(null);
    try {
      const res = await notificationsApi.sendTestNotification();
      if (res.success) {
        setTestResult({
          success: true,
          message: `Test email dispatched to ${res.data.recipient}. Please check your inbox or local Mailpit viewer (port 8025).`,
        });
      } else {
        setTestResult({
          success: false,
          message: res.message || 'Mail server returned failure. Please verify SMTP host and port.',
        });
      }
    } catch (err: any) {
      setTestResult({
        success: false,
        message: err.response?.data?.message || 'Failed to trigger test notification dispatch',
      });
    } finally {
      setTestSending(false);
    }
  };

  const getBadgeStyle = (type: NotificationType) => {
    switch (type) {
      case 'SERVICE_UNHEALTHY':
      case 'INCIDENT_OPENED':
        return 'bg-rose-50 text-rose-700 dark:bg-rose-500/10 dark:text-rose-400 border border-rose-200 dark:border-rose-500/30';
      case 'SERVICE_RECOVERED':
      case 'INCIDENT_RESOLVED':
        return 'bg-emerald-50 text-emerald-700 dark:bg-emerald-500/10 dark:text-emerald-400 border border-emerald-200 dark:border-emerald-500/30';
      case 'TEST':
      default:
        return 'bg-sky-50 text-sky-700 dark:bg-sky-500/10 dark:text-sky-400 border border-sky-200 dark:border-sky-500/30';
    }
  };

  const formatNotificationType = (type: NotificationType) => {
    switch (type) {
      case 'SERVICE_UNHEALTHY':
        return 'Service Unhealthy';
      case 'SERVICE_RECOVERED':
        return 'Service Recovered';
      case 'INCIDENT_OPENED':
        return 'Incident Opened';
      case 'INCIDENT_RESOLVED':
        return 'Incident Resolved';
      case 'TEST':
        return 'Test Notification';
      default:
        return type;
    }
  };

  return (
    <div className="flex-1 flex flex-col pb-16">
      <Navbar
        title="Email Notifications & Alerting"
        subtitle="Manage dispatch rules, email destinations, and inspect historical alert deliveries"
      />

      <main className="px-8 mt-8 space-y-6 max-w-6xl">
        {/* Navigation Tabs */}
        <div className="flex border-b border-slate-200 dark:border-slate-800 gap-4">
          <button
            onClick={() => setActiveTab('settings')}
            className={`pb-3 px-2 flex items-center gap-2 text-sm font-semibold border-b-2 transition-colors ${
              activeTab === 'settings'
                ? 'border-sky-600 text-sky-600 dark:border-sky-400 dark:text-sky-400'
                : 'border-transparent text-slate-500 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-200'
            }`}
          >
            <Bell className="w-4 h-4" />
            <span>Alert Preferences</span>
          </button>
          <button
            onClick={() => setActiveTab('history')}
            className={`pb-3 px-2 flex items-center gap-2 text-sm font-semibold border-b-2 transition-colors ${
              activeTab === 'history'
                ? 'border-sky-600 text-sky-600 dark:border-sky-400 dark:text-sky-400'
                : 'border-transparent text-slate-500 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-200'
            }`}
          >
            <History className="w-4 h-4" />
            <span>Delivery History</span>
            {historyTotalElements > 0 && (
              <span className="text-xs px-2 py-0.5 rounded-full bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300">
                {historyTotalElements}
              </span>
            )}
          </button>
        </div>

        {/* TAB 1: Alert Preferences */}
        {activeTab === 'settings' && (
          <div className="space-y-6">
            {prefsLoading ? (
              <div className="p-12 text-center text-slate-400 bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-800">
                <RefreshCw className="w-6 h-6 animate-spin mx-auto mb-2 text-sky-600" />
                <p className="text-sm">Loading notification configuration...</p>
              </div>
            ) : (
              <form onSubmit={handleSavePreferences} className="space-y-6">
                {/* Feedback Banners */}
                {prefsSuccess && (
                  <div className="p-4 bg-emerald-50 dark:bg-emerald-500/10 border border-emerald-200 dark:border-emerald-500/30 rounded-xl flex items-center gap-3 text-sm text-emerald-800 dark:text-emerald-300">
                    <CheckCircle2 className="w-5 h-5 shrink-0 text-emerald-600 dark:text-emerald-400" />
                    <span>{prefsSuccess}</span>
                  </div>
                )}
                {prefsError && (
                  <div className="p-4 bg-rose-50 dark:bg-rose-500/10 border border-rose-200 dark:border-rose-500/30 rounded-xl flex items-center gap-3 text-sm text-rose-800 dark:text-rose-300">
                    <AlertCircle className="w-5 h-5 shrink-0 text-rose-600 dark:text-rose-400" />
                    <span>{prefsError}</span>
                  </div>
                )}

                {/* Master Switch Card */}
                <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-6 shadow-xl transition-colors duration-200">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-lg bg-sky-50 dark:bg-sky-600/20 text-sky-600 dark:text-sky-400 flex items-center justify-center">
                        <Mail className="w-5 h-5" />
                      </div>
                      <div>
                        <h3 className="text-base font-bold text-slate-900 dark:text-white">Email Notification Engine</h3>
                        <p className="text-xs text-slate-500 dark:text-slate-400">
                          Master control to enable or silence all outgoing alert emails across all monitors
                        </p>
                      </div>
                    </div>
                    <label className="relative inline-flex items-center cursor-pointer">
                      <input
                        type="checkbox"
                        checked={emailEnabled}
                        onChange={(e) => setEmailEnabled(e.target.checked)}
                        className="sr-only peer"
                      />
                      <div className="w-11 h-6 bg-slate-200 dark:bg-slate-700 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-slate-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-sky-600"></div>
                    </label>
                  </div>

                  {/* Destination Email */}
                  <div className="mt-6 pt-6 border-t border-slate-200 dark:border-slate-800">
                    <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 uppercase tracking-wider mb-1">
                      Alert Destination Email
                    </label>
                    <p className="text-xs text-slate-500 dark:text-slate-400 mb-2">
                      Leave blank to default to your account email address (<code className="text-sky-600 dark:text-sky-400">{preferences?.notificationEmail || 'user account email'}</code>)
                    </p>
                    <input
                      type="email"
                      value={notificationEmail}
                      onChange={(e) => setNotificationEmail(e.target.value)}
                      placeholder="e.g. devops-alerts@mycompany.com"
                      className="w-full md:w-96 px-3.5 py-2.5 rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-950 text-slate-900 dark:text-white text-sm focus:ring-2 focus:ring-sky-500 outline-none"
                    />
                  </div>
                </div>

                {/* Event Trigger Preferences Card */}
                <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-6 shadow-xl transition-colors duration-200">
                  <div className="flex items-center gap-3 border-b border-slate-200 dark:border-slate-800 pb-4 mb-5">
                    <div className="w-10 h-10 rounded-lg bg-indigo-50 dark:bg-indigo-600/20 text-indigo-600 dark:text-indigo-400 flex items-center justify-center">
                      <Zap className="w-5 h-5" />
                    </div>
                    <div>
                      <h3 className="text-base font-bold text-slate-900 dark:text-white">Trigger Conditions</h3>
                      <p className="text-xs text-slate-500 dark:text-slate-400">
                        Select which platform events generate and dispatch an email alert
                      </p>
                    </div>
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    {/* Trigger: Incident Opened */}
                    <div className="p-4 rounded-xl border border-slate-200 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-950/40 flex items-start justify-between gap-4">
                      <div className="flex items-start gap-3">
                        <ShieldAlert className="w-5 h-5 text-rose-600 dark:text-rose-400 shrink-0 mt-0.5" />
                        <div>
                          <span className="text-sm font-bold text-slate-900 dark:text-white block">Incident Opened</span>
                          <span className="text-xs text-slate-500 dark:text-slate-400">
                            Dispatched when consecutive check failures reach the downtime threshold (e.g. 3 failures)
                          </span>
                        </div>
                      </div>
                      <input
                        type="checkbox"
                        checked={notifyOnIncidentOpened}
                        disabled={!emailEnabled}
                        onChange={(e) => setNotifyOnIncidentOpened(e.target.checked)}
                        className="w-4 h-4 text-sky-600 rounded border-slate-300 dark:border-slate-700 mt-1 cursor-pointer disabled:opacity-50"
                      />
                    </div>

                    {/* Trigger: Incident Resolved */}
                    <div className="p-4 rounded-xl border border-slate-200 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-950/40 flex items-start justify-between gap-4">
                      <div className="flex items-start gap-3">
                        <CheckCircle2 className="w-5 h-5 text-emerald-600 dark:text-emerald-400 shrink-0 mt-0.5" />
                        <div>
                          <span className="text-sm font-bold text-slate-900 dark:text-white block">Incident Resolved</span>
                          <span className="text-xs text-slate-500 dark:text-slate-400">
                            Dispatched when an open downtime incident recovers, including calculated downtime duration
                          </span>
                        </div>
                      </div>
                      <input
                        type="checkbox"
                        checked={notifyOnIncidentResolved}
                        disabled={!emailEnabled}
                        onChange={(e) => setNotifyOnIncidentResolved(e.target.checked)}
                        className="w-4 h-4 text-sky-600 rounded border-slate-300 dark:border-slate-700 mt-1 cursor-pointer disabled:opacity-50"
                      />
                    </div>

                    {/* Trigger: Service Unhealthy */}
                    <div className="p-4 rounded-xl border border-slate-200 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-950/40 flex items-start justify-between gap-4">
                      <div className="flex items-start gap-3">
                        <AlertTriangle className="w-5 h-5 text-amber-600 dark:text-amber-400 shrink-0 mt-0.5" />
                        <div>
                          <span className="text-sm font-bold text-slate-900 dark:text-white block">Service Becomes Unhealthy</span>
                          <span className="text-xs text-slate-500 dark:text-slate-400">
                            Immediate notification as soon as a monitor health transitions from Healthy to Unhealthy
                          </span>
                        </div>
                      </div>
                      <input
                        type="checkbox"
                        checked={notifyOnUnhealthy}
                        disabled={!emailEnabled}
                        onChange={(e) => setNotifyOnUnhealthy(e.target.checked)}
                        className="w-4 h-4 text-sky-600 rounded border-slate-300 dark:border-slate-700 mt-1 cursor-pointer disabled:opacity-50"
                      />
                    </div>

                    {/* Trigger: Service Recovery */}
                    <div className="p-4 rounded-xl border border-slate-200 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-950/40 flex items-start justify-between gap-4">
                      <div className="flex items-start gap-3">
                        <Sparkles className="w-5 h-5 text-sky-600 dark:text-sky-400 shrink-0 mt-0.5" />
                        <div>
                          <span className="text-sm font-bold text-slate-900 dark:text-white block">Service Recovers</span>
                          <span className="text-xs text-slate-500 dark:text-slate-400">
                            Immediate alert when health status returns to Healthy following an outage or check failure
                          </span>
                        </div>
                      </div>
                      <input
                        type="checkbox"
                        checked={notifyOnRecovery}
                        disabled={!emailEnabled}
                        onChange={(e) => setNotifyOnRecovery(e.target.checked)}
                        className="w-4 h-4 text-sky-600 rounded border-slate-300 dark:border-slate-700 mt-1 cursor-pointer disabled:opacity-50"
                      />
                    </div>
                  </div>
                </div>

                {/* Action Buttons */}
                <div className="flex flex-wrap items-center justify-between gap-4">
                  <button
                    type="submit"
                    disabled={prefsSaving}
                    className="px-6 py-2.5 rounded-lg bg-sky-600 hover:bg-sky-700 text-white font-semibold text-sm shadow-lg shadow-sky-600/20 disabled:opacity-60 flex items-center gap-2 transition-colors cursor-pointer"
                  >
                    {prefsSaving && <RefreshCw className="w-4 h-4 animate-spin" />}
                    <span>{prefsSaving ? 'Saving Changes...' : 'Save Notification Preferences'}</span>
                  </button>

                  <button
                    type="button"
                    onClick={handleSendTestNotification}
                    disabled={testSending || !emailEnabled}
                    className="px-4 py-2.5 rounded-lg border border-slate-300 dark:border-slate-700 hover:bg-slate-100 dark:hover:bg-slate-800 text-slate-700 dark:text-slate-200 font-medium text-sm flex items-center gap-2 transition-colors disabled:opacity-50 cursor-pointer"
                  >
                    {testSending ? <RefreshCw className="w-4 h-4 animate-spin" /> : <Send className="w-4 h-4 text-sky-600 dark:text-sky-400" />}
                    <span>{testSending ? 'Sending Test Email...' : 'Send Test Alert Email'}</span>
                  </button>
                </div>

                {/* Test Result Message */}
                {testResult && (
                  <div
                    className={`p-4 rounded-xl border text-sm flex items-start gap-3 ${
                      testResult.success
                        ? 'bg-emerald-50 dark:bg-emerald-500/10 border-emerald-200 dark:border-emerald-500/30 text-emerald-800 dark:text-emerald-300'
                        : 'bg-rose-50 dark:bg-rose-500/10 border-rose-200 dark:border-rose-500/30 text-rose-800 dark:text-rose-300'
                    }`}
                  >
                    {testResult.success ? (
                      <CheckCircle2 className="w-5 h-5 shrink-0 text-emerald-600 dark:text-emerald-400 mt-0.5" />
                    ) : (
                      <AlertCircle className="w-5 h-5 shrink-0 text-rose-600 dark:text-rose-400 mt-0.5" />
                    )}
                    <div>
                      <p className="font-semibold">{testResult.success ? 'Delivery Succeeded' : 'Delivery Failed'}</p>
                      <p className="text-xs mt-0.5">{testResult.message}</p>
                    </div>
                  </div>
                )}
              </form>
            )}
          </div>
        )}

        {/* TAB 2: Delivery History */}
        {activeTab === 'history' && (
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <h3 className="text-base font-bold text-slate-900 dark:text-white">Notification Audit History</h3>
                <p className="text-xs text-slate-500 dark:text-slate-400">
                  Track idempotent dispatch events, recipient destinations, and delivery statuses
                </p>
              </div>
              <button
                onClick={() => fetchHistory(historyPage)}
                disabled={historyLoading}
                className="p-2 rounded-lg border border-slate-200 dark:border-slate-800 text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors cursor-pointer"
                title="Refresh audit history"
              >
                <RefreshCw className={`w-4 h-4 ${historyLoading ? 'animate-spin' : ''}`} />
              </button>
            </div>

            {historyError && (
              <div className="p-4 bg-rose-50 dark:bg-rose-500/10 border border-rose-200 dark:border-rose-500/30 rounded-xl flex items-center gap-3 text-sm text-rose-800 dark:text-rose-300">
                <AlertCircle className="w-5 h-5 shrink-0 text-rose-600 dark:text-rose-400" />
                <span>{historyError}</span>
              </div>
            )}

            {historyLoading && history.length === 0 ? (
              <div className="p-12 text-center text-slate-400 bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-800">
                <RefreshCw className="w-6 h-6 animate-spin mx-auto mb-2 text-sky-600" />
                <p className="text-sm">Loading delivery records...</p>
              </div>
            ) : history.length === 0 ? (
              <div className="p-12 text-center bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-800">
                <Clock className="w-10 h-10 mx-auto text-slate-300 dark:text-slate-700 mb-3" />
                <h4 className="text-sm font-bold text-slate-900 dark:text-white">No Notification Records Yet</h4>
                <p className="text-xs text-slate-500 dark:text-slate-400 mt-1 max-w-sm mx-auto">
                  Notification dispatch entries will appear here once incident or service health state changes occur, or after sending a test notification.
                </p>
                <button
                  onClick={handleSendTestNotification}
                  className="mt-4 px-4 py-2 text-xs font-semibold rounded-lg bg-sky-600 text-white hover:bg-sky-700 transition-colors cursor-pointer inline-flex items-center gap-1.5"
                >
                  <Send className="w-3.5 h-3.5" />
                  <span>Send Test Alert</span>
                </button>
              </div>
            ) : (
              <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl shadow-xl overflow-hidden transition-colors duration-200">
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-xs">
                    <thead className="bg-slate-50 dark:bg-slate-950 border-b border-slate-200 dark:border-slate-800 text-slate-500 dark:text-slate-400 uppercase font-semibold">
                      <tr>
                        <th className="px-5 py-3">Event Type</th>
                        <th className="px-5 py-3">Target Service</th>
                        <th className="px-5 py-3">Recipient</th>
                        <th className="px-5 py-3">Subject</th>
                        <th className="px-5 py-3">Status</th>
                        <th className="px-5 py-3">Timestamp (UTC)</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100 dark:divide-slate-800/60 font-medium">
                      {history.map((item) => (
                        <tr key={item.id} className="hover:bg-slate-50/60 dark:hover:bg-slate-800/40 transition-colors">
                          <td className="px-5 py-3.5">
                            <span className={`inline-block px-2.5 py-1 rounded-full text-[11px] font-bold ${getBadgeStyle(item.notificationType)}`}>
                              {formatNotificationType(item.notificationType)}
                            </span>
                          </td>
                          <td className="px-5 py-3.5 text-slate-800 dark:text-slate-200">
                            {item.monitorName ? (
                              <div>
                                <span className="font-semibold block">{item.monitorName}</span>
                                <span className="text-[10px] text-slate-400 font-mono truncate max-w-[200px] block">
                                  {item.monitorUrl}
                                </span>
                              </div>
                            ) : (
                              <span className="text-slate-400 italic">System Engine</span>
                            )}
                          </td>
                          <td className="px-5 py-3.5 font-mono text-slate-600 dark:text-slate-300">
                            {item.recipient}
                          </td>
                          <td className="px-5 py-3.5 text-slate-700 dark:text-slate-300 max-w-xs truncate" title={item.subject}>
                            {item.subject}
                          </td>
                          <td className="px-5 py-3.5">
                            {item.status === 'SENT' ? (
                              <span className="inline-flex items-center gap-1 text-emerald-600 dark:text-emerald-400 font-semibold">
                                <CheckCircle2 className="w-3.5 h-3.5" />
                                <span>SENT</span>
                              </span>
                            ) : item.status === 'SKIPPED' ? (
                              <span
                                className="inline-flex items-center gap-1 text-slate-500 dark:text-slate-400 font-semibold cursor-help"
                                title={item.failureReason || 'Notification skipped per preferences'}
                              >
                                <Clock className="w-3.5 h-3.5" />
                                <span>SKIPPED</span>
                              </span>
                            ) : item.status === 'IN_FLIGHT' ? (
                              <span className="inline-flex items-center gap-1 text-sky-600 dark:text-sky-400 font-semibold">
                                <RefreshCw className="w-3.5 h-3.5 animate-spin" />
                                <span>IN FLIGHT</span>
                              </span>
                            ) : item.status === 'PENDING' ? (
                              <span className="inline-flex items-center gap-1 text-amber-600 dark:text-amber-400 font-semibold">
                                <Clock className="w-3.5 h-3.5" />
                                <span>PENDING</span>
                              </span>
                            ) : (
                              <span
                                className="inline-flex items-center gap-1 text-rose-600 dark:text-rose-400 font-semibold cursor-help"
                                title={item.failureReason || 'Failed to dispatch'}
                              >
                                <AlertCircle className="w-3.5 h-3.5" />
                                <span>FAILED</span>
                              </span>
                            )}
                          </td>
                          <td className="px-5 py-3.5 text-slate-500 dark:text-slate-400 font-mono">
                            {new Date(item.sentAt).toLocaleString('en-US', { timeZone: 'UTC' })} UTC
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>

                {/* Pagination Controls */}
                {historyTotalPages > 1 && (
                  <div className="p-4 border-t border-slate-200 dark:border-slate-800 flex items-center justify-between text-xs text-slate-500 dark:text-slate-400">
                    <span>
                      Showing Page {historyPage + 1} of {historyTotalPages} ({historyTotalElements} total records)
                    </span>
                    <div className="flex items-center gap-2">
                      <button
                        onClick={() => setHistoryPage((p) => Math.max(0, p - 1))}
                        disabled={historyPage === 0 || historyLoading}
                        className="px-3 py-1.5 rounded-lg border border-slate-200 dark:border-slate-800 disabled:opacity-40 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
                      >
                        Previous
                      </button>
                      <button
                        onClick={() => setHistoryPage((p) => Math.min(historyTotalPages - 1, p + 1))}
                        disabled={historyPage >= historyTotalPages - 1 || historyLoading}
                        className="px-3 py-1.5 rounded-lg border border-slate-200 dark:border-slate-800 disabled:opacity-40 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
                      >
                        Next
                      </button>
                    </div>
                  </div>
                )}
              </div>
            )}
          </div>
        )}
      </main>
    </div>
  );
};
