import React, { useEffect, useState } from 'react';
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import {
  Activity,
  AlertOctagon,
  AlertTriangle,
  CheckCircle2,
  Clock,
  Gauge,
  Loader2,
  ShieldAlert,
  Zap,
} from 'lucide-react';
import api from '../api/client';
import { Navbar } from '../components/layout/Navbar';
import { useTheme } from '../context/ThemeContext';
import type { AdvancedAnalytics, Anomaly, AnomalySeverity, DashboardSummary } from '../types';

export const Analytics: React.FC = () => {
  const { chartColors } = useTheme();
  const [data, setData] = useState<DashboardSummary | null>(null);
  const [advanced, setAdvanced] = useState<AdvancedAnalytics | null>(null);
  const [anomalies, setAnomalies] = useState<Anomaly[]>([]);
  const [loading, setLoading] = useState(true);
  const [acknowledgingId, setAcknowledgingId] = useState<number | null>(null);
  const [filterType, setFilterType] = useState<'ALL' | 'UNACK' | 'LATENCY_SPIKE' | 'FAILURE_BURST' | 'UPTIME_DEGRADATION'>('ALL');

  const fetchAnalytics = async () => {
    try {
      const [dashRes, advRes, anomRes] = await Promise.all([
        api.get('/analytics/dashboard'),
        api.get('/analytics/advanced'),
        api.get('/anomalies?size=50'),
      ]);
      setData(dashRes.data.data);
      setAdvanced(advRes.data.data);
      setAnomalies(anomRes.data.data?.content || []);
    } catch (err) {
      console.error('Failed to load analytics', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAnalytics();
  }, []);

  const handleAcknowledge = async (anomalyId: number) => {
    try {
      setAcknowledgingId(anomalyId);
      await api.post(`/anomalies/${anomalyId}/acknowledge`);
      setAnomalies((prev) =>
        prev.map((a) => (a.id === anomalyId ? { ...a, acknowledged: true, acknowledgedAt: new Date().toISOString() } : a))
      );
      if (advanced) {
        setAdvanced({
          ...advanced,
          unacknowledgedAnomalies: Math.max(0, advanced.unacknowledgedAnomalies - 1),
        });
      }
    } catch (err) {
      console.error('Failed to acknowledge anomaly', err);
    } finally {
      setAcknowledgingId(null);
    }
  };

  const getSeverityBadge = (severity: AnomalySeverity) => {
    switch (severity) {
      case 'CRITICAL':
        return 'bg-red-500/10 text-red-600 dark:text-red-400 border-red-500/20';
      case 'HIGH':
        return 'bg-rose-500/10 text-rose-600 dark:text-rose-400 border-rose-500/20';
      case 'MEDIUM':
        return 'bg-amber-500/10 text-amber-600 dark:text-amber-400 border-amber-500/20';
      case 'LOW':
        return 'bg-sky-500/10 text-sky-600 dark:text-sky-400 border-sky-500/20';
      default:
        return 'bg-slate-500/10 text-slate-600 dark:text-slate-400 border-slate-500/20';
    }
  };

  const getTypeBadge = (type: string) => {
    switch (type) {
      case 'LATENCY_SPIKE':
        return { label: 'Latency Spike', icon: Zap, color: 'text-amber-500' };
      case 'FAILURE_BURST':
        return { label: 'Failure Burst', icon: ShieldAlert, color: 'text-rose-500' };
      case 'UPTIME_DEGRADATION':
        return { label: 'Uptime Degraded', icon: AlertOctagon, color: 'text-orange-500' };
      default:
        return { label: type, icon: AlertTriangle, color: 'text-slate-500' };
    }
  };

  const filteredAnomalies = anomalies.filter((a) => {
    if (filterType === 'UNACK') return !a.acknowledged;
    if (filterType === 'LATENCY_SPIKE') return a.anomalyType === 'LATENCY_SPIKE';
    if (filterType === 'FAILURE_BURST') return a.anomalyType === 'FAILURE_BURST';
    if (filterType === 'UPTIME_DEGRADATION') return a.anomalyType === 'UPTIME_DEGRADATION';
    return true;
  });

  if (loading) {
    return (
      <div className="flex-1 flex items-center justify-center min-h-[500px]">
        <div className="w-8 h-8 border-4 border-sky-500 border-t-transparent rounded-full animate-spin"></div>
      </div>
    );
  }

  const pieData = [
    { name: 'Successful Checks', value: (data?.totalChecks || 0) - (data?.failedChecks || 0), color: '#10b981' },
    { name: 'Failed Checks', value: data?.failedChecks || 0, color: '#f43f5e' },
  ];

  return (
    <div className="flex-1 flex flex-col pb-16">
      <Navbar
        title="Analytics & Telemetry"
        subtitle="Historical verification statistics, reliability metrics, and statistical anomaly detection"
        onRefresh={fetchAnalytics}
      />

      <main className="px-8 mt-8 space-y-8 max-w-7xl">
        {/* Metric Overview Row (4 KPI Cards) */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
          {/* Card 1: SLA Availability */}
          <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-5 shadow-sm transition-colors duration-200">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400 block mb-1">
              Overall SLA Uptime
            </span>
            <div className="flex items-baseline gap-2">
              <span className="text-3xl font-extrabold text-slate-900 dark:text-white">
                {advanced?.overallUptimePercentage ?? data?.overallUptimePercentage ?? 100}%
              </span>
            </div>
            <div className="mt-3 pt-3 border-t border-slate-100 dark:border-slate-800 flex justify-between text-xs text-slate-500 dark:text-slate-400">
              <span>24h: <strong className="text-slate-700 dark:text-slate-200">{advanced?.uptime24h != null ? `${advanced.uptime24h}%` : 'N/A'}</strong></span>
              <span>7d: <strong className="text-slate-700 dark:text-slate-200">{advanced?.uptime7d != null ? `${advanced.uptime7d}%` : 'N/A'}</strong></span>
              <span>30d: <strong className="text-slate-700 dark:text-slate-200">{advanced?.uptime30d != null ? `${advanced.uptime30d}%` : 'N/A'}</strong></span>
            </div>
          </div>

          {/* Card 2: Mean Time to Recovery (MTTR) */}
          <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-5 shadow-sm transition-colors duration-200">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">
                Mean Time to Recovery (MTTR)
              </span>
              <Clock className="w-4 h-4 text-sky-500" />
            </div>
            <div className="mt-1 flex items-baseline gap-2">
              <span className="text-3xl font-extrabold text-sky-600 dark:text-sky-400 font-mono">
                {advanced?.mttrMinutes != null ? `${advanced.mttrMinutes}m` : '0m'}
              </span>
            </div>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-3 pt-3 border-t border-slate-100 dark:border-slate-800">
              Avg resolution time from incident open to recovery
            </p>
          </div>

          {/* Card 3: Mean Time Between Failures (MTBF) */}
          <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-5 shadow-sm transition-colors duration-200">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">
                Mean Time Between Failures
              </span>
              <Gauge className="w-4 h-4 text-emerald-500" />
            </div>
            <div className="mt-1 flex items-baseline gap-2">
              <span className="text-3xl font-extrabold text-emerald-600 dark:text-emerald-400 font-mono">
                {advanced?.mtbfHours != null ? `${advanced.mtbfHours}h` : '720h'}
              </span>
            </div>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-3 pt-3 border-t border-slate-100 dark:border-slate-800">
              Operational uptime hours per incident event
            </p>
          </div>

          {/* Card 4: Tail Latencies (P95 / P99) */}
          <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-5 shadow-sm transition-colors duration-200">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">
                P95 / P99 Tail Latency
              </span>
              <Activity className="w-4 h-4 text-indigo-500" />
            </div>
            <div className="mt-1 flex items-baseline gap-3">
              <span className="text-3xl font-extrabold text-slate-900 dark:text-white font-mono">
                {advanced?.p95LatencyMs != null ? `${advanced.p95LatencyMs}` : '0'}
                <span className="text-xs font-normal text-slate-500 ml-1">ms (P95)</span>
              </span>
            </div>
            <div className="mt-3 pt-3 border-t border-slate-100 dark:border-slate-800 flex justify-between text-xs text-slate-500 dark:text-slate-400">
              <span>Avg: <strong className="text-slate-700 dark:text-slate-200">{advanced?.avgLatencyMs ?? data?.averageResponseTimeMs ?? 0}ms</strong></span>
              <span>P99: <strong className="text-slate-700 dark:text-slate-200">{advanced?.p99LatencyMs != null ? `${advanced.p99LatencyMs}ms` : 'N/A'}</strong></span>
            </div>
          </div>
        </div>

        {/* Charts Row */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Chart 1: Hourly Latency Bar Chart */}
          <div className="lg:col-span-2 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-6 shadow-sm transition-colors duration-200">
            <div className="flex items-center justify-between mb-4">
              <div>
                <h3 className="text-base font-bold text-slate-900 dark:text-white tracking-tight">
                  Hourly Average Response Time (24h)
                </h3>
                <p className="text-xs text-slate-500 dark:text-slate-400">
                  Aggregated across all active endpoints in 1-hour UTC buckets
                </p>
              </div>
            </div>

            <div className="h-64 w-full">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={advanced?.trends || data?.responseTimeTrends || []} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke={chartColors.grid} vertical={false} />
                  <XAxis dataKey="timestamp" stroke={chartColors.axis} fontSize={11} tickLine={false} />
                  <YAxis stroke={chartColors.axis} fontSize={11} tickLine={false} unit="ms" />
                  <Tooltip
                    contentStyle={{
                      backgroundColor: chartColors.tooltipBg,
                      borderColor: chartColors.tooltipBorder,
                      borderRadius: '8px',
                      color: chartColors.tooltipText,
                      fontSize: '12px',
                      boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)',
                    }}
                    itemStyle={{ color: chartColors.tooltipText }}
                    labelStyle={{ color: chartColors.tooltipText, fontWeight: 'bold' }}
                    formatter={(val: any) => [`${val} ms`, 'Avg Latency']}
                  />
                  <Bar dataKey="avgResponseTimeMs" fill="#0284c7" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>

          {/* Chart 2: Success vs Failure Distribution */}
          <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-6 shadow-sm flex flex-col justify-between transition-colors duration-200">
            <div>
              <h3 className="text-base font-bold text-slate-900 dark:text-white tracking-tight mb-1">Check Result Ratio</h3>
              <p className="text-xs text-slate-500 dark:text-slate-400 mb-6">Pass vs Fail execution distribution</p>

              <div className="h-52 w-full flex items-center justify-center">
                {data && data.totalChecks > 0 ? (
                  <ResponsiveContainer width="100%" height="100%">
                    <PieChart>
                      <Pie
                        data={pieData}
                        cx="50%"
                        cy="50%"
                        innerRadius={50}
                        outerRadius={75}
                        paddingAngle={5}
                        dataKey="value"
                      >
                        {pieData.map((entry, index) => (
                          <Cell key={`cell-${index}`} fill={entry.color} />
                        ))}
                      </Pie>
                      <Tooltip
                        contentStyle={{
                          backgroundColor: chartColors.tooltipBg,
                          borderColor: chartColors.tooltipBorder,
                          borderRadius: '8px',
                          color: chartColors.tooltipText,
                          fontSize: '12px',
                          boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)',
                        }}
                        itemStyle={{ color: chartColors.tooltipText }}
                        labelStyle={{ color: chartColors.tooltipText, fontWeight: 'bold' }}
                      />
                    </PieChart>
                  </ResponsiveContainer>
                ) : (
                  <p className="text-xs text-slate-400 dark:text-slate-500">No telemetry recorded yet</p>
                )}
              </div>
            </div>

            <div className="flex items-center justify-around text-xs border-t border-slate-200 dark:border-slate-800 pt-4">
              <span className="flex items-center gap-2 text-emerald-600 dark:text-emerald-400 font-semibold">
                <span className="w-2.5 h-2.5 rounded-full bg-emerald-500"></span>
                Success ({((data?.totalChecks || 0) - (data?.failedChecks || 0))})
              </span>
              <span className="flex items-center gap-2 text-rose-600 dark:text-rose-400 font-semibold">
                <span className="w-2.5 h-2.5 rounded-full bg-rose-500"></span>
                Failures ({data?.failedChecks || 0})
              </span>
            </div>
          </div>
        </div>

        {/* Phase 2.4 Anomaly Detection Section */}
        <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-6 shadow-sm transition-colors duration-200 space-y-6">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-200 dark:border-slate-800 pb-5">
            <div>
              <div className="flex items-center gap-3">
                <h3 className="text-lg font-bold text-slate-900 dark:text-white tracking-tight flex items-center gap-2">
                  <ShieldAlert className="w-5 h-5 text-amber-500" />
                  Statistical Anomaly Detection Center
                </h3>
                {advanced && advanced.unacknowledgedAnomalies > 0 && (
                  <span className="px-2.5 py-0.5 text-xs font-bold rounded-full bg-amber-500/10 text-amber-600 dark:text-amber-400 border border-amber-500/20 animate-pulse">
                    {advanced.unacknowledgedAnomalies} Unacknowledged
                  </span>
                )}
              </div>
              <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
                Real-time automated detection of latency spikes (Z-score &ge; 3.0), failure bursts, and rolling uptime degradation.
              </p>
            </div>

            {/* Filter Tabs */}
            <div className="flex flex-wrap items-center gap-1.5">
              {(
                [
                  { id: 'ALL', label: 'All Anomalies' },
                  { id: 'UNACK', label: 'Unacknowledged' },
                  { id: 'LATENCY_SPIKE', label: 'Latency Spikes' },
                  { id: 'FAILURE_BURST', label: 'Failure Bursts' },
                  { id: 'UPTIME_DEGRADATION', label: 'Uptime Drops' },
                ] as const
              ).map((tab) => (
                <button
                  key={tab.id}
                  type="button"
                  onClick={() => setFilterType(tab.id)}
                  className={`px-3 py-1.5 text-xs font-medium rounded-lg transition-colors ${
                    filterType === tab.id
                      ? 'bg-primary-600 text-white shadow-sm'
                      : 'text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800'
                  }`}
                >
                  {tab.label}
                </button>
              ))}
            </div>
          </div>

          {/* Anomalies List */}
          {filteredAnomalies.length === 0 ? (
            <div className="text-center py-12">
              <CheckCircle2 className="w-10 h-10 text-emerald-500 mx-auto mb-3" />
              <h4 className="text-sm font-semibold text-slate-900 dark:text-white">
                No active anomalies detected
              </h4>
              <p className="text-xs text-slate-500 dark:text-slate-400 mt-1 max-w-sm mx-auto">
                All monitoring telemetry conforms to statistical baselines with zero unacknowledged anomalies.
              </p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead>
                  <tr className="border-b border-slate-200 dark:border-slate-800 text-slate-500 dark:text-slate-400 uppercase font-semibold">
                    <th className="py-3 px-4">Severity</th>
                    <th className="py-3 px-4">Type</th>
                    <th className="py-3 px-4">Monitor</th>
                    <th className="py-3 px-4">Observed vs Baseline</th>
                    <th className="py-3 px-4">Description</th>
                    <th className="py-3 px-4">Detected</th>
                    <th className="py-3 px-4 text-right">Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                  {filteredAnomalies.map((anom) => {
                    const typeConfig = getTypeBadge(anom.anomalyType);
                    const TypeIcon = typeConfig.icon;
                    return (
                      <tr
                        key={anom.id}
                        className={`hover:bg-slate-50/50 dark:hover:bg-slate-800/50 transition-colors ${
                          !anom.acknowledged ? 'bg-amber-50/20 dark:bg-amber-950/10' : ''
                        }`}
                      >
                        <td className="py-3 px-4 whitespace-nowrap">
                          <span
                            className={`px-2 py-0.5 text-[11px] font-bold rounded-md border ${getSeverityBadge(
                              anom.severity
                            )}`}
                          >
                            {anom.severity}
                          </span>
                        </td>
                        <td className="py-3 px-4 whitespace-nowrap">
                          <span className="flex items-center gap-1.5 font-medium text-slate-800 dark:text-slate-200">
                            <TypeIcon className={`w-3.5 h-3.5 ${typeConfig.color}`} />
                            {typeConfig.label}
                          </span>
                        </td>
                        <td className="py-3 px-4 whitespace-nowrap font-medium text-slate-900 dark:text-white">
                          <div>{anom.monitorName}</div>
                          <div className="text-[10px] text-slate-400 truncate max-w-[180px]">
                            {anom.monitorUrl}
                          </div>
                        </td>
                        <td className="py-3 px-4 whitespace-nowrap font-mono text-slate-700 dark:text-slate-300">
                          <span className="font-bold text-rose-600 dark:text-rose-400">
                            {anom.observedValue}
                          </span>
                          <span className="text-slate-400 mx-1">/</span>
                          <span className="text-slate-500">
                            {anom.baselineValue}
                          </span>
                        </td>
                        <td className="py-3 px-4 text-slate-600 dark:text-slate-300 max-w-xs">
                          {anom.description}
                        </td>
                        <td className="py-3 px-4 whitespace-nowrap text-slate-500 dark:text-slate-400">
                          {new Date(anom.detectedAt).toLocaleString()}
                        </td>
                        <td className="py-3 px-4 whitespace-nowrap text-right">
                          {anom.acknowledged ? (
                            <span className="inline-flex items-center gap-1 text-[11px] text-emerald-600 dark:text-emerald-400 font-medium">
                              <CheckCircle2 className="w-3.5 h-3.5" />
                              Acknowledged
                            </span>
                          ) : (
                            <button
                              type="button"
                              onClick={() => handleAcknowledge(anom.id)}
                              disabled={acknowledgingId === anom.id}
                              className="px-2.5 py-1 text-[11px] font-semibold rounded-md bg-slate-100 hover:bg-slate-200 dark:bg-slate-800 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-200 transition-colors disabled:opacity-50 cursor-pointer"
                            >
                              {acknowledgingId === anom.id ? (
                                <Loader2 className="w-3 h-3 animate-spin inline" />
                              ) : (
                                'Acknowledge'
                              )}
                            </button>
                          )}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </main>
    </div>
  );
};

export default Analytics;
