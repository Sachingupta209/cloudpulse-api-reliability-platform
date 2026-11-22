import React, { useEffect, useState } from 'react';
import {
  FileText,
  Download,
  Calendar,
  Layers,
  CheckCircle2,
  AlertTriangle,
  Activity,
  Shield,
  Loader2,
  Clock,
} from 'lucide-react';
import api from '../api/client';
import type { Monitor } from '../types';

type ReportType = 'MONITOR' | 'SUMMARY' | 'INCIDENT' | 'PERFORMANCE';
type DatePreset = '24h' | '7d' | '30d' | '90d' | 'custom';

export const Reports: React.FC = () => {
  const [reportType, setReportType] = useState<ReportType>('SUMMARY');
  const [monitors, setMonitors] = useState<Monitor[]>([]);
  const [selectedMonitorId, setSelectedMonitorId] = useState<number | ''>('');
  const [datePreset, setDatePreset] = useState<DatePreset>('7d');
  const [customStartDate, setCustomStartDate] = useState('');
  const [customEndDate, setCustomEndDate] = useState('');
  const [loadingMonitors, setLoadingMonitors] = useState(true);
  const [downloading, setDownloading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  useEffect(() => {
    fetchMonitors();
  }, []);

  const fetchMonitors = async () => {
    try {
      setLoadingMonitors(true);
      const res = await api.get('/monitors');
      const monitorList = res.data.data || [];
      setMonitors(monitorList);
      if (monitorList.length > 0) {
        setSelectedMonitorId(monitorList[0].id);
      }
    } catch (err: any) {
      console.error('Failed to load monitors', err);
      setError('Failed to load monitors. Please refresh the page.');
    } finally {
      setLoadingMonitors(false);
    }
  };

  const calculateDateRange = (): { start?: string; end?: string } => {
    const now = new Date();
    if (datePreset === 'custom') {
      return {
        start: customStartDate ? new Date(customStartDate).toISOString() : undefined,
        end: customEndDate ? new Date(customEndDate).toISOString() : undefined,
      };
    }

    const end = now.toISOString();
    let start: string;
    switch (datePreset) {
      case '24h':
        start = new Date(now.getTime() - 24 * 60 * 60 * 1000).toISOString();
        break;
      case '7d':
        start = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000).toISOString();
        break;
      case '30d':
        start = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000).toISOString();
        break;
      case '90d':
        start = new Date(now.getTime() - 90 * 24 * 60 * 60 * 1000).toISOString();
        break;
      default:
        start = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000).toISOString();
    }
    return { start, end };
  };

  const handleDownload = async () => {
    setError(null);
    setSuccess(null);

    if (reportType === 'MONITOR' && !selectedMonitorId) {
      setError('Please select a monitor to generate the report.');
      return;
    }

    const { start, end } = calculateDateRange();
    if (datePreset === 'custom') {
      if (!start || !end) {
        setError('Please choose both start and end dates for custom range.');
        return;
      }
      if (new Date(start) > new Date(end)) {
        setError('Start date cannot be after end date.');
        return;
      }
    }

    setDownloading(true);

    try {
      let endpoint = '';
      let defaultFilename = 'cloudpulse_report.pdf';
      const params: Record<string, string> = {};
      if (start) params.startDate = start;
      if (end) params.endDate = end;

      switch (reportType) {
        case 'MONITOR':
          endpoint = `/reports/monitors/${selectedMonitorId}/pdf`;
          defaultFilename = `cloudpulse_monitor_${selectedMonitorId}_report.pdf`;
          break;
        case 'SUMMARY':
          endpoint = '/reports/summary/pdf';
          defaultFilename = 'cloudpulse_portfolio_summary.pdf';
          break;
        case 'INCIDENT':
          endpoint = '/reports/incidents/pdf';
          defaultFilename = 'cloudpulse_incident_log.pdf';
          break;
        case 'PERFORMANCE':
          endpoint = '/reports/performance/pdf';
          defaultFilename = 'cloudpulse_performance_sla.pdf';
          break;
      }

      const response = await api.get(endpoint, {
        params,
        responseType: 'blob',
      });

      const disposition = response.headers['content-disposition'];
      let filename = defaultFilename;
      if (disposition && disposition.indexOf('filename=') !== -1) {
        const matches = /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/.exec(disposition);
        if (matches != null && matches[1]) {
          filename = matches[1].replace(/['"]/g, '');
        }
      }

      const blob = new Blob([response.data], { type: 'application/pdf' });
      const downloadUrl = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = downloadUrl;
      link.setAttribute('download', filename);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(downloadUrl);

      setSuccess(`Report "${filename}" downloaded successfully!`);
    } catch (err: any) {
      console.error('Download error:', err);
      setError(err?.response?.data?.message || 'Failed to generate PDF report. Please try again.');
    } finally {
      setDownloading(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div>
        <h1 className="text-2xl font-bold text-slate-900 dark:text-white flex items-center gap-2">
          <FileText className="w-6 h-6 text-primary-500" />
          PDF Monitoring Reports
        </h1>
        <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">
          Generate audit-ready, high-resolution PDF SLA and incident reports for compliance, stakeholders, and operational audits.
        </p>
      </div>

      {/* Notifications / Alerts */}
      {error && (
        <div className="p-4 rounded-xl bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 text-red-700 dark:text-red-300 flex items-center gap-3">
          <AlertTriangle className="w-5 h-5 flex-shrink-0" />
          <p className="text-sm font-medium">{error}</p>
        </div>
      )}

      {success && (
        <div className="p-4 rounded-xl bg-emerald-50 dark:bg-emerald-900/20 border border-emerald-200 dark:border-emerald-800 text-emerald-700 dark:text-emerald-300 flex items-center gap-3">
          <CheckCircle2 className="w-5 h-5 flex-shrink-0" />
          <p className="text-sm font-medium">{success}</p>
        </div>
      )}

      {/* Configuration Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left Column: Report Type Selection */}
        <div className="lg:col-span-2 space-y-6">
          <div className="bg-white dark:bg-slate-800 rounded-2xl border border-slate-200 dark:border-slate-700 p-6 shadow-sm">
            <h2 className="text-base font-semibold text-slate-900 dark:text-white mb-4 flex items-center gap-2">
              <Layers className="w-4 h-4 text-primary-500" />
              1. Select Report Template
            </h2>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {/* Portfolio Summary Card */}
              <button
                type="button"
                onClick={() => setReportType('SUMMARY')}
                className={`p-4 rounded-xl border text-left transition-all ${
                  reportType === 'SUMMARY'
                    ? 'border-primary-500 bg-primary-50/50 dark:bg-primary-950/20 ring-2 ring-primary-500/20'
                    : 'border-slate-200 dark:border-slate-700 hover:border-slate-300 dark:hover:border-slate-600 bg-slate-50/50 dark:bg-slate-900/50'
                }`}
              >
                <div className="flex items-center gap-3 mb-2">
                  <div className="w-9 h-9 rounded-lg bg-indigo-500/10 text-indigo-500 flex items-center justify-center font-bold">
                    <Shield className="w-5 h-5" />
                  </div>
                  <div>
                    <h3 className="font-semibold text-sm text-slate-900 dark:text-white">Portfolio Summary</h3>
                    <p className="text-xs text-slate-500 dark:text-slate-400">Executive Overview</p>
                  </div>
                </div>
                <p className="text-xs text-slate-600 dark:text-slate-300 mt-2 leading-relaxed">
                  Consolidated health overview of all monitors, aggregate SLA availability, overall uptime percentage, and incident summaries.
                </p>
              </button>

              {/* Single Monitor SLA Report Card */}
              <button
                type="button"
                onClick={() => setReportType('MONITOR')}
                className={`p-4 rounded-xl border text-left transition-all ${
                  reportType === 'MONITOR'
                    ? 'border-primary-500 bg-primary-50/50 dark:bg-primary-950/20 ring-2 ring-primary-500/20'
                    : 'border-slate-200 dark:border-slate-700 hover:border-slate-300 dark:hover:border-slate-600 bg-slate-50/50 dark:bg-slate-900/50'
                }`}
              >
                <div className="flex items-center gap-3 mb-2">
                  <div className="w-9 h-9 rounded-lg bg-emerald-500/10 text-emerald-500 flex items-center justify-center font-bold">
                    <CheckCircle2 className="w-5 h-5" />
                  </div>
                  <div>
                    <h3 className="font-semibold text-sm text-slate-900 dark:text-white">Single Monitor SLA</h3>
                    <p className="text-xs text-slate-500 dark:text-slate-400">Endpoint Deep Dive</p>
                  </div>
                </div>
                <p className="text-xs text-slate-600 dark:text-slate-300 mt-2 leading-relaxed">
                  Detailed uptime metrics, HTTP response times, latency distribution (P95, P99), and downtime event history for a specific endpoint.
                </p>
              </button>

              {/* Incident & Downtime Audit Card */}
              <button
                type="button"
                onClick={() => setReportType('INCIDENT')}
                className={`p-4 rounded-xl border text-left transition-all ${
                  reportType === 'INCIDENT'
                    ? 'border-primary-500 bg-primary-50/50 dark:bg-primary-950/20 ring-2 ring-primary-500/20'
                    : 'border-slate-200 dark:border-slate-700 hover:border-slate-300 dark:hover:border-slate-600 bg-slate-50/50 dark:bg-slate-900/50'
                }`}
              >
                <div className="flex items-center gap-3 mb-2">
                  <div className="w-9 h-9 rounded-lg bg-amber-500/10 text-amber-500 flex items-center justify-center font-bold">
                    <AlertTriangle className="w-5 h-5" />
                  </div>
                  <div>
                    <h3 className="font-semibold text-sm text-slate-900 dark:text-white">Incident Audit Log</h3>
                    <p className="text-xs text-slate-500 dark:text-slate-400">Downtime & Post-Mortem</p>
                  </div>
                </div>
                <p className="text-xs text-slate-600 dark:text-slate-300 mt-2 leading-relaxed">
                  Chronological incident log, root cause notes, MTTR (Mean Time to Recovery), and downtime durations for compliance audits.
                </p>
              </button>

              {/* Latency & Performance SLA Card */}
              <button
                type="button"
                onClick={() => setReportType('PERFORMANCE')}
                className={`p-4 rounded-xl border text-left transition-all ${
                  reportType === 'PERFORMANCE'
                    ? 'border-primary-500 bg-primary-50/50 dark:bg-primary-950/20 ring-2 ring-primary-500/20'
                    : 'border-slate-200 dark:border-slate-700 hover:border-slate-300 dark:hover:border-slate-600 bg-slate-50/50 dark:bg-slate-900/50'
                }`}
              >
                <div className="flex items-center gap-3 mb-2">
                  <div className="w-9 h-9 rounded-lg bg-cyan-500/10 text-cyan-500 flex items-center justify-center font-bold">
                    <Activity className="w-5 h-5" />
                  </div>
                  <div>
                    <h3 className="font-semibold text-sm text-slate-900 dark:text-white">Performance & Latency</h3>
                    <p className="text-xs text-slate-500 dark:text-slate-400">SLA & Latency Percentiles</p>
                  </div>
                </div>
                <p className="text-xs text-slate-600 dark:text-slate-300 mt-2 leading-relaxed">
                  Response latency percentiles (P95, P99, Min, Max, Avg), throughput statistics, and SLA latency threshold comparisons.
                </p>
              </button>
            </div>

            {/* Monitor Dropdown (if Single Monitor selected) */}
            {reportType === 'MONITOR' && (
              <div className="mt-6 pt-6 border-t border-slate-200 dark:border-slate-700">
                <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-2">
                  Select Monitor <span className="text-red-500">*</span>
                </label>
                {loadingMonitors ? (
                  <div className="flex items-center gap-2 text-sm text-slate-500">
                    <Loader2 className="w-4 h-4 animate-spin" />
                    Loading your monitors...
                  </div>
                ) : monitors.length === 0 ? (
                  <p className="text-sm text-amber-600 dark:text-amber-400">
                    No monitors found. Please create a monitor first.
                  </p>
                ) : (
                  <select
                    value={selectedMonitorId}
                    onChange={(e) => setSelectedMonitorId(Number(e.target.value))}
                    className="w-full px-3 py-2 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg text-sm text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-primary-500"
                  >
                    {monitors.map((m) => (
                      <option key={m.id} value={m.id}>
                        {m.name} ({m.url})
                      </option>
                    ))}
                  </select>
                )}
              </div>
            )}
          </div>

          {/* Date Range Selection */}
          <div className="bg-white dark:bg-slate-800 rounded-2xl border border-slate-200 dark:border-slate-700 p-6 shadow-sm">
            <h2 className="text-base font-semibold text-slate-900 dark:text-white mb-4 flex items-center gap-2">
              <Calendar className="w-4 h-4 text-primary-500" />
              2. Select Reporting Period
            </h2>

            {/* Presets */}
            <div className="grid grid-cols-2 sm:grid-cols-5 gap-2 mb-4">
              {(['24h', '7d', '30d', '90d', 'custom'] as DatePreset[]).map((preset) => (
                <button
                  key={preset}
                  type="button"
                  onClick={() => setDatePreset(preset)}
                  className={`py-2 px-3 text-xs font-semibold rounded-lg border transition-all text-center capitalize ${
                    datePreset === preset
                      ? 'border-primary-500 bg-primary-50 text-primary-700 dark:bg-primary-950/40 dark:text-primary-300'
                      : 'border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-300 hover:bg-slate-50 dark:hover:bg-slate-700'
                  }`}
                >
                  {preset === '24h'
                    ? 'Last 24 Hours'
                    : preset === '7d'
                    ? 'Last 7 Days'
                    : preset === '30d'
                    ? 'Last 30 Days'
                    : preset === '90d'
                    ? 'Last 90 Days'
                    : 'Custom Range'}
                </button>
              ))}
            </div>

            {/* Custom Range Inputs */}
            {datePreset === 'custom' && (
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 pt-4 border-t border-slate-200 dark:border-slate-700">
                <div>
                  <label className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
                    Start Date & Time
                  </label>
                  <input
                    type="datetime-local"
                    value={customStartDate}
                    onChange={(e) => setCustomStartDate(e.target.value)}
                    className="w-full px-3 py-2 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg text-sm text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-primary-500"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
                    End Date & Time
                  </label>
                  <input
                    type="datetime-local"
                    value={customEndDate}
                    onChange={(e) => setCustomEndDate(e.target.value)}
                    className="w-full px-3 py-2 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg text-sm text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-primary-500"
                  />
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Right Column: Summary Card & Action Button */}
        <div className="space-y-6">
          <div className="bg-white dark:bg-slate-800 rounded-2xl border border-slate-200 dark:border-slate-700 p-6 shadow-sm">
            <h2 className="text-base font-semibold text-slate-900 dark:text-white mb-4">
              Report Specification
            </h2>

            <dl className="space-y-3 text-sm">
              <div className="flex justify-between py-1 border-b border-slate-100 dark:border-slate-700/50">
                <dt className="text-slate-500 dark:text-slate-400">Template</dt>
                <dd className="font-medium text-slate-900 dark:text-white capitalize">
                  {reportType.toLowerCase()}
                </dd>
              </div>
              <div className="flex justify-between py-1 border-b border-slate-100 dark:border-slate-700/50">
                <dt className="text-slate-500 dark:text-slate-400">Timeframe</dt>
                <dd className="font-medium text-slate-900 dark:text-white">
                  {datePreset === '24h'
                    ? '24 Hours'
                    : datePreset === '7d'
                    ? '7 Days'
                    : datePreset === '30d'
                    ? '30 Days'
                    : datePreset === '90d'
                    ? '90 Days'
                    : 'Custom'}
                </dd>
              </div>
              <div className="flex justify-between py-1 border-b border-slate-100 dark:border-slate-700/50">
                <dt className="text-slate-500 dark:text-slate-400">Output Format</dt>
                <dd className="font-medium text-slate-900 dark:text-white">PDF / ISO Standard</dd>
              </div>
              <div className="flex justify-between py-1">
                <dt className="text-slate-500 dark:text-slate-400">Renderer</dt>
                <dd className="font-medium text-slate-900 dark:text-white">OpenPDF 2.0.3</dd>
              </div>
            </dl>

            <div className="mt-6">
              <button
                type="button"
                onClick={handleDownload}
                disabled={downloading || (reportType === 'MONITOR' && !selectedMonitorId)}
                className="w-full py-3 px-4 rounded-xl bg-primary-600 hover:bg-primary-700 text-white font-medium text-sm flex items-center justify-center gap-2 shadow-sm transition-all disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
              >
                {downloading ? (
                  <>
                    <Loader2 className="w-4 h-4 animate-spin" />
                    Generating PDF Report...
                  </>
                ) : (
                  <>
                    <Download className="w-4 h-4" />
                    Generate & Download PDF
                  </>
                )}
              </button>
            </div>
          </div>

          {/* Quick Help Card */}
          <div className="bg-slate-50 dark:bg-slate-900/60 rounded-2xl border border-slate-200 dark:border-slate-800 p-5">
            <h3 className="text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400 mb-2 flex items-center gap-1.5">
              <Clock className="w-3.5 h-3.5" />
              Compliance Note
            </h3>
            <p className="text-xs text-slate-600 dark:text-slate-400 leading-relaxed">
              PDF reports include timestamped check results, verified SLA uptime percentages, latency percentiles (P95, P99), and incident resolution logs suitable for SOC 2 and ISO 27001 audit compliance.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Reports;
