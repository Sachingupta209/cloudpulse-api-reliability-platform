import React, { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import {
  AlertCircle,
  ArrowLeft,
  CheckCircle2,
  Code2,
  Edit3,
  Pause,
  Play,
  RefreshCw,
  Trash2,
  XCircle,
} from 'lucide-react';
import {
  Area,
  AreaChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import api from '../api/client';
import { Navbar } from '../components/layout/Navbar';
import { useTheme } from '../context/ThemeContext';
import type { CheckResult, Monitor } from '../types';

export const MonitorDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { chartColors } = useTheme();

  const [monitor, setMonitor] = useState<Monitor | null>(null);
  const [results, setResults] = useState<CheckResult[]>([]);
  const [loading, setLoading] = useState(true);
  const [checking, setChecking] = useState(false);

  const fetchDetails = async () => {
    try {
      const monRes = await api.get(`/monitors/${id}`);
      setMonitor(monRes.data.data);

      try {
        const resRes = await api.get(`/monitors/${id}/results?page=0&size=30`);
        setResults(resRes.data.data?.content || resRes.data.data || []);
      } catch (resultsErr) {
        console.warn('Could not load monitor check results', resultsErr);
        setResults([]);
      }
    } catch (err) {
      console.error('Failed to load monitor details', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDetails();
  }, [id]);

  const handleManualCheck = async () => {
    try {
      setChecking(true);
      await api.post(`/monitors/${id}/check`);
      await fetchDetails();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Manual health check failed');
    } finally {
      setChecking(false);
    }
  };

  const handleTogglePause = async () => {
    if (!monitor) return;
    try {
      if (monitor.status === 'ACTIVE') {
        await api.patch(`/monitors/${id}/pause`);
      } else {
        await api.patch(`/monitors/${id}/resume`);
      }
      await fetchDetails();
    } catch (err) {
      console.error('Failed to toggle monitor status', err);
    }
  };

  const handleDelete = async () => {
    if (!monitor) return;
    if (confirm(`Are you sure you want to delete monitor "${monitor.name}"?`)) {
      try {
        await api.delete(`/monitors/${id}`);
        navigate('/monitors');
      } catch (err) {
        console.error('Failed to delete monitor', err);
      }
    }
  };

  if (loading) {
    return (
      <div className="flex-1 flex items-center justify-center">
        <div className="w-8 h-8 border-4 border-sky-500 border-t-transparent rounded-full animate-spin"></div>
      </div>
    );
  }

  if (!monitor) {
    return (
      <div className="flex-1 flex flex-col items-center justify-center text-center p-8">
        <AlertCircle className="w-12 h-12 text-slate-400 dark:text-slate-500 mb-3" />
        <h3 className="text-lg font-bold text-slate-900 dark:text-white">Monitor Not Found</h3>
        <button
          onClick={() => navigate('/monitors')}
          className="mt-4 px-4 py-2 bg-slate-100 dark:bg-slate-800 text-sky-600 dark:text-sky-400 rounded-lg text-sm font-semibold"
        >
          Return to monitors
        </button>
      </div>
    );
  }

  // Format chart data from recent results in chronological order
  const chartData = [...results].reverse().map((r) => ({
    timestamp: new Date(r.checkedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' }),
    latency: r.responseTimeMs,
    status: r.status,
  }));

  return (
    <div className="flex-1 flex flex-col pb-16">
      <Navbar
        title={monitor.name}
        subtitle={`${monitor.httpMethod} ${monitor.url}`}
        onRefresh={fetchDetails}
        isRefreshing={checking}
      />

      <main className="px-8 mt-8 space-y-8 max-w-7xl">
        {/* Navigation & Action Header */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <button
            onClick={() => navigate('/monitors')}
            className="flex items-center gap-2 text-xs font-semibold text-slate-500 dark:text-slate-400 hover:text-slate-800 dark:hover:text-slate-200 transition-colors"
          >
            <ArrowLeft className="w-4 h-4" />
            <span>Back to all monitors</span>
          </button>

          <div className="flex items-center gap-2.5">
            <button
              onClick={handleManualCheck}
              disabled={checking}
              className="flex items-center gap-2 px-3.5 py-2 bg-sky-600 hover:bg-sky-500 text-white text-xs font-semibold rounded-lg shadow-lg shadow-sky-600/30 transition-all disabled:opacity-50"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${checking ? 'animate-spin' : ''}`} />
              <span>Check Now</span>
            </button>

            <button
              onClick={handleTogglePause}
              className="flex items-center gap-1.5 px-3.5 py-2 bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-200 text-xs font-semibold rounded-lg border border-slate-200 dark:border-slate-700 transition-colors"
            >
              {monitor.status === 'ACTIVE' ? <Pause className="w-3.5 h-3.5" /> : <Play className="w-3.5 h-3.5" />}
              <span>{monitor.status === 'ACTIVE' ? 'Pause' : 'Resume'}</span>
            </button>

            <Link
              to={`/monitors/${id}/edit`}
              className="flex items-center gap-1.5 px-3.5 py-2 bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-200 text-xs font-semibold rounded-lg border border-slate-200 dark:border-slate-700 transition-colors"
            >
              <Edit3 className="w-3.5 h-3.5 text-sky-600 dark:text-sky-400" />
              <span>Edit</span>
            </Link>

            <button
              onClick={handleDelete}
              className="p-2 text-slate-400 hover:text-rose-600 dark:hover:text-rose-400 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg transition-colors border border-slate-200 dark:border-slate-800"
              title="Delete monitor"
            >
              <Trash2 className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Overview Stats Bar */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-5">
          <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-5 shadow-lg transition-colors duration-200">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400 block mb-1">
              Current Health
            </span>
            <span
              className={`text-xl font-extrabold inline-flex items-center gap-2 ${
                monitor.status === 'PAUSED'
                  ? 'text-amber-600 dark:text-amber-400'
                  : monitor.healthStatus === 'HEALTHY'
                  ? 'text-emerald-600 dark:text-emerald-400'
                  : monitor.healthStatus === 'UNHEALTHY'
                  ? 'text-rose-600 dark:text-rose-400'
                  : 'text-slate-500 dark:text-slate-400'
              }`}
            >
              <span
                className={`w-2.5 h-2.5 rounded-full ${
                  monitor.status === 'PAUSED'
                    ? 'bg-amber-400'
                    : monitor.healthStatus === 'HEALTHY'
                    ? 'bg-emerald-500'
                    : 'bg-rose-500'
                }`}
              />
              {monitor.status === 'PAUSED' ? 'Paused' : monitor.healthStatus}
            </span>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-2">
              Consecutive failures: <span className="text-slate-800 dark:text-slate-200 font-semibold">{monitor.consecutiveFailures}</span>
            </p>
          </div>

          <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-5 shadow-lg transition-colors duration-200">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400 block mb-1">
              24h Uptime
            </span>
            <span className="text-xl font-extrabold text-slate-900 dark:text-white">
              {monitor.uptimePercentage != null ? `${monitor.uptimePercentage.toFixed(1)}%` : '100%'}
            </span>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-2">Interval: every {monitor.checkIntervalSeconds}s</p>
          </div>

          <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-5 shadow-lg transition-colors duration-200">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400 block mb-1">
              Avg Latency (24h)
            </span>
            <span className="text-xl font-extrabold text-slate-900 dark:text-white">
              {monitor.avgResponseTimeMs != null ? `${Math.round(monitor.avgResponseTimeMs)} ms` : '—'}
            </span>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-2">Timeout: {monitor.timeoutMs}ms</p>
          </div>

          <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-5 shadow-lg transition-colors duration-200">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400 block mb-1">
              Expected HTTP Status
            </span>
            <span className="text-xl font-extrabold text-sky-600 dark:text-sky-400 font-mono">
              {monitor.expectedStatusCode} OK
            </span>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-2">
              Last check:{' '}
              {monitor.lastCheckedAt
                ? new Date(monitor.lastCheckedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })
                : 'Never'}
            </p>
          </div>
        </div>

        {/* Latency History Chart */}
        <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-6 shadow-xl transition-colors duration-200">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h3 className="text-base font-bold text-slate-900 dark:text-white tracking-tight">Recent Check Response Times</h3>
              <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">Response latency profile in milliseconds</p>
            </div>
            <div className="flex items-center gap-2">
              <span className="w-2.5 h-2.5 rounded-full bg-sky-500"></span>
              <span className="text-xs text-slate-500 dark:text-slate-400 font-medium">Latency (ms)</span>
            </div>
          </div>

          <div className="h-60 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={chartData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                <defs>
                  <linearGradient id="monLatency" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#0284c7" stopOpacity={0.4} />
                    <stop offset="95%" stopColor="#0284c7" stopOpacity={0.0} />
                  </linearGradient>
                </defs>
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
                  formatter={(value: any) => [`${value} ms`, 'Response Time']}
                />
                <Area
                  type="monotone"
                  dataKey="latency"
                  stroke="#0284c7"
                  strokeWidth={2}
                  fillOpacity={1}
                  fill="url(#monLatency)"
                />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Validation Rules Card */}
        <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-6 shadow-xl transition-colors duration-200">
          <h3 className="text-base font-bold text-slate-900 dark:text-white tracking-tight flex items-center gap-2 mb-4">
            <Code2 className="w-5 h-5 text-sky-600 dark:text-sky-400" />
            Configured Smart Validation Rules ({monitor.validationRules.length})
          </h3>

          {monitor.validationRules.length > 0 ? (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {monitor.validationRules.map((rule, i) => (
                <div key={i} className="p-3.5 bg-slate-50 dark:bg-slate-950/70 border border-slate-200 dark:border-slate-800 rounded-lg space-y-1.5">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-bold text-sky-600 dark:text-sky-400 font-mono">{rule.ruleType}</span>
                    <span className="text-[10px] px-2 py-0.5 bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 rounded-full font-semibold">
                      Active
                    </span>
                  </div>
                  {rule.targetExpression && (
                    <p className="text-xs font-mono text-slate-700 dark:text-slate-300">
                      Expression: <span className="text-sky-700 dark:text-sky-300 font-semibold">{rule.targetExpression}</span>
                    </p>
                  )}
                  {rule.expectedValue && (
                    <p className="text-xs font-mono text-slate-700 dark:text-slate-300">
                      Expected: <span className="text-emerald-600 dark:text-emerald-400 font-semibold">{rule.expectedValue}</span>
                    </p>
                  )}
                  {rule.errorMessage && (
                    <p className="text-[11px] text-slate-500 dark:text-slate-400 italic">"{rule.errorMessage}"</p>
                  )}
                </div>
              ))}
            </div>
          ) : (
            <p className="text-xs text-slate-500">No custom validation rules configured.</p>
          )}
        </div>

        {/* Execution History Table */}
        <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl overflow-hidden shadow-xl transition-colors duration-200">
          <div className="px-6 py-4 border-b border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-950/40 flex items-center justify-between">
            <h3 className="text-base font-bold text-slate-900 dark:text-white tracking-tight">Execution Log (Recent 30 Checks)</h3>
            <span className="text-xs text-slate-500 dark:text-slate-400">Recorded in database</span>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-sm">
              <thead>
                <tr className="border-b border-slate-200 dark:border-slate-800 text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">
                  <th className="py-3 px-6">Timestamp (UTC)</th>
                  <th className="py-3 px-4">Result</th>
                  <th className="py-3 px-4">HTTP Status</th>
                  <th className="py-3 px-4">Latency</th>
                  <th className="py-3 px-4">Target IP</th>
                  <th className="py-3 px-6">Failure Diagnostics</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200 dark:divide-slate-800/80">
                {results.map((r) => (
                  <tr key={r.id} className="hover:bg-slate-50 dark:hover:bg-slate-800/30 transition-colors">
                    <td className="py-3.5 px-6 font-mono text-xs text-slate-700 dark:text-slate-300">
                      {new Date(r.checkedAt).toISOString().replace('T', ' ').substring(0, 19)}
                    </td>
                    <td className="py-3.5 px-4">
                      <span
                        className={`text-xs px-2.5 py-0.5 rounded-full font-semibold inline-flex items-center gap-1 ${
                          r.status === 'SUCCESS'
                            ? 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-500/30'
                            : 'bg-rose-500/10 text-rose-600 dark:text-rose-400 border border-rose-500/30'
                        }`}
                      >
                        {r.status === 'SUCCESS' ? (
                          <CheckCircle2 className="w-3.5 h-3.5" />
                        ) : (
                          <XCircle className="w-3.5 h-3.5" />
                        )}
                        {r.status}
                      </span>
                    </td>
                    <td className="py-3.5 px-4 font-mono text-xs text-slate-700 dark:text-slate-300">
                      {r.httpStatusCode || '—'}
                    </td>
                    <td className="py-3.5 px-4 font-mono text-xs text-slate-700 dark:text-slate-300">
                      {r.responseTimeMs} ms
                    </td>
                    <td className="py-3.5 px-4 font-mono text-xs text-slate-500 dark:text-slate-400">
                      {r.ipAddress || '—'}
                    </td>
                    <td className="py-3.5 px-6 text-xs text-rose-600 dark:text-rose-400">
                      {r.failureReason || <span className="text-slate-400 dark:text-slate-500">—</span>}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </main>
    </div>
  );
};
