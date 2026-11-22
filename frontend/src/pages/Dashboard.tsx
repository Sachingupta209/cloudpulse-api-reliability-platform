import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  Activity,
  AlertOctagon,
  ArrowUpRight,
  CheckCircle2,
  Clock,
  Layers,
  PauseCircle,
  PlusCircle,
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
import type { DashboardSummary } from '../types';

export const Dashboard: React.FC = () => {
  const { chartColors } = useTheme();
  const [data, setData] = useState<DashboardSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);

  const fetchDashboard = async () => {
    try {
      setRefreshing(true);
      const res = await api.get('/analytics/dashboard');
      setData(res.data.data);
    } catch (err) {
      console.error('Failed to load dashboard metrics', err);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    fetchDashboard();
    const interval = setInterval(fetchDashboard, 30000);
    return () => clearInterval(interval);
  }, []);

  if (loading) {
    return (
      <div className="flex-1 flex items-center justify-center">
        <div className="w-8 h-8 border-4 border-sky-500 border-t-transparent rounded-full animate-spin"></div>
      </div>
    );
  }

  const chartData = data?.responseTimeTrends && data.responseTimeTrends.length > 0
    ? data.responseTimeTrends
    : [
        { timestamp: '00:00', avgResponseTimeMs: 0 },
        { timestamp: '04:00', avgResponseTimeMs: 0 },
        { timestamp: '08:00', avgResponseTimeMs: 0 },
        { timestamp: '12:00', avgResponseTimeMs: 0 },
        { timestamp: '16:00', avgResponseTimeMs: 0 },
        { timestamp: '20:00', avgResponseTimeMs: 0 },
      ];

  return (
    <div className="flex-1 flex flex-col pb-12">
      <Navbar
        title="Reliability Dashboard"
        subtitle="Real-time API uptime, response time metrics, and active incidents"
        onRefresh={fetchDashboard}
        isRefreshing={refreshing}
      />

      <main className="px-8 mt-8 space-y-8 max-w-7xl">
        {/* KPI Stat Cards */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-5">
          {/* Card 1: Overall Uptime */}
          <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-5 shadow-lg relative overflow-hidden transition-colors duration-200">
            <div className="flex items-center justify-between text-slate-500 dark:text-slate-400 mb-2">
              <span className="text-xs font-semibold uppercase tracking-wider">Overall Uptime</span>
              <Activity className="w-4 h-4 text-emerald-500 dark:text-emerald-400" />
            </div>
            <div className="flex items-baseline gap-2">
              <span className="text-3xl font-extrabold text-slate-900 dark:text-white">
                {data?.overallUptimePercentage ?? 100}%
              </span>
              <span className="text-xs text-emerald-600 dark:text-emerald-400 font-medium">Last 24h</span>
            </div>
            <div className="mt-3 w-full bg-slate-100 dark:bg-slate-800 h-1.5 rounded-full overflow-hidden">
              <div
                className="bg-emerald-500 h-full rounded-full transition-all duration-500"
                style={{ width: `${data?.overallUptimePercentage ?? 100}%` }}
              ></div>
            </div>
          </div>

          {/* Card 2: Monitors State */}
          <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-5 shadow-lg transition-colors duration-200">
            <div className="flex items-center justify-between text-slate-500 dark:text-slate-400 mb-2">
              <span className="text-xs font-semibold uppercase tracking-wider">Monitors State</span>
              <Layers className="w-4 h-4 text-sky-500 dark:text-sky-400" />
            </div>
            <div className="flex items-baseline gap-2">
              <span className="text-3xl font-extrabold text-slate-900 dark:text-white">{data?.totalMonitors ?? 0}</span>
              <span className="text-xs text-slate-500 dark:text-slate-400">Total</span>
            </div>
            <div className="mt-3 flex items-center gap-3 text-xs">
              <span className="flex items-center gap-1 text-emerald-600 dark:text-emerald-400 font-medium">
                <CheckCircle2 className="w-3.5 h-3.5" /> {data?.healthyMonitors ?? 0}
              </span>
              <span className="flex items-center gap-1 text-rose-600 dark:text-rose-400 font-medium">
                <XCircle className="w-3.5 h-3.5" /> {data?.unhealthyMonitors ?? 0}
              </span>
              <span className="flex items-center gap-1 text-amber-600 dark:text-amber-400 font-medium">
                <PauseCircle className="w-3.5 h-3.5" /> {data?.pausedMonitors ?? 0}
              </span>
            </div>
          </div>

          {/* Card 3: Avg Response Time */}
          <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-5 shadow-lg transition-colors duration-200">
            <div className="flex items-center justify-between text-slate-500 dark:text-slate-400 mb-2">
              <span className="text-xs font-semibold uppercase tracking-wider">Avg Latency</span>
              <Clock className="w-4 h-4 text-sky-500 dark:text-sky-400" />
            </div>
            <div className="flex items-baseline gap-2">
              <span className="text-3xl font-extrabold text-slate-900 dark:text-white">
                {data?.averageResponseTimeMs ?? 0}
              </span>
              <span className="text-sm font-semibold text-slate-500 dark:text-slate-400">ms</span>
            </div>
            <p className="mt-3 text-xs text-slate-500 dark:text-slate-400">
              Total checks executed:{' '}
              <span className="font-semibold text-slate-800 dark:text-slate-200">{data?.totalChecks ?? 0}</span>
            </p>
          </div>

          {/* Card 4: Active Incidents */}
          <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-5 shadow-lg transition-colors duration-200">
            <div className="flex items-center justify-between text-slate-500 dark:text-slate-400 mb-2">
              <span className="text-xs font-semibold uppercase tracking-wider">Active Incidents</span>
              <AlertOctagon className="w-4 h-4 text-rose-500 dark:text-rose-400" />
            </div>
            <div className="flex items-baseline gap-2">
              <span
                className={`text-3xl font-extrabold ${
                  (data?.activeIncidents ?? 0) > 0 ? 'text-rose-600 dark:text-rose-400' : 'text-slate-900 dark:text-slate-100'
                }`}
              >
                {data?.activeIncidents ?? 0}
              </span>
              <span className="text-xs text-slate-500 dark:text-slate-400">Open</span>
            </div>
            <p className="mt-3 text-xs text-slate-500 dark:text-slate-400">
              Failed checks:{' '}
              <span className="font-semibold text-rose-600 dark:text-rose-400">{data?.failedChecks ?? 0}</span>
            </p>
          </div>
        </div>

        {/* Latency History Chart */}
        <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-6 shadow-xl transition-colors duration-200">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h3 className="text-base font-bold text-slate-900 dark:text-white tracking-tight">Average Response Time (24h)</h3>
              <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">Calculated in UTC time-window across monitored endpoints</p>
            </div>
            <div className="flex items-center gap-2">
              <span className="w-2.5 h-2.5 rounded-full bg-sky-500"></span>
              <span className="text-xs text-slate-600 dark:text-slate-400 font-medium">Latency (ms)</span>
            </div>
          </div>

          <div className="h-64 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={chartData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                <defs>
                  <linearGradient id="latencyGradient" x1="0" y1="0" x2="0" y2="1">
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
                  formatter={(value: any) => [`${value} ms`, 'Avg Latency']}
                />
                <Area
                  type="monotone"
                  dataKey="avgResponseTimeMs"
                  stroke="#0284c7"
                  strokeWidth={2}
                  fillOpacity={1}
                  fill="url(#latencyGradient)"
                />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Recent Incidents and Quick Setup */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Recent Incidents */}
          <div className="lg:col-span-2 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-6 shadow-xl transition-colors duration-200">
            <div className="flex items-center justify-between mb-5">
              <div>
                <h3 className="text-base font-bold text-slate-900 dark:text-white tracking-tight">Recent Incidents</h3>
                <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">Automated detection after 3 consecutive failures</p>
              </div>
              <Link
                to="/incidents"
                className="text-xs text-sky-600 dark:text-sky-400 hover:text-sky-500 font-medium flex items-center gap-1"
              >
                View all <ArrowUpRight className="w-3.5 h-3.5" />
              </Link>
            </div>

            {data?.recentIncidents && data.recentIncidents.length > 0 ? (
              <div className="divide-y divide-slate-200 dark:divide-slate-800">
                {data.recentIncidents.map((incident) => (
                  <div key={incident.id} className="py-3.5 flex items-start justify-between gap-4">
                    <div className="space-y-1">
                      <div className="flex items-center gap-2">
                        <span
                          className={`text-xs px-2 py-0.5 rounded font-semibold ${
                            incident.status === 'OPEN'
                              ? 'bg-rose-500/10 text-rose-600 dark:text-rose-400 border border-rose-500/30'
                              : 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-500/30'
                          }`}
                        >
                          {incident.status}
                        </span>
                        <span className="text-sm font-semibold text-slate-900 dark:text-slate-200">{incident.title}</span>
                      </div>
                      <p className="text-xs text-slate-500 dark:text-slate-400 line-clamp-1">{incident.rootCause || 'Root cause unidentified'}</p>
                    </div>
                    <div className="text-right shrink-0 text-xs text-slate-500 dark:text-slate-400">
                      <span>{new Date(incident.startedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
                      {incident.downtimeSeconds != null && (
                        <p className="text-slate-500 dark:text-slate-400 mt-0.5">Duration: {incident.downtimeSeconds}s</p>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="py-12 flex flex-col items-center justify-center text-center">
                <CheckCircle2 className="w-10 h-10 text-emerald-500/50 mb-3" />
                <p className="text-sm font-semibold text-slate-800 dark:text-slate-300">All systems operating normally</p>
                <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">No active or recent downtime incidents recorded.</p>
              </div>
            )}
          </div>

          {/* Quick Monitor Launcher */}
          <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-6 shadow-xl flex flex-col justify-between transition-colors duration-200">
            <div>
              <h3 className="text-base font-bold text-slate-900 dark:text-white tracking-tight">Active Probes</h3>
              <p className="text-xs text-slate-500 dark:text-slate-400 mt-1 mb-6">
                CloudPulse monitors both transport connectivity and application-level JSON payload assertions.
              </p>

              <div className="space-y-3">
                <div className="p-3 bg-slate-50 dark:bg-slate-950/60 rounded-lg border border-slate-200 dark:border-slate-800/80">
                  <span className="text-xs font-semibold text-slate-800 dark:text-slate-300">Smart JSON Validation</span>
                  <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
                    Detects HTTP 200 responses that carry hidden database or auth errors in JSON.
                  </p>
                </div>
                <div className="p-3 bg-slate-50 dark:bg-slate-950/60 rounded-lg border border-slate-200 dark:border-slate-800/80">
                  <span className="text-xs font-semibold text-slate-800 dark:text-slate-300">SSRF Hardened</span>
                  <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
                    Internal RFC 1918 networks and cloud metadata IP addresses are rigorously blocked.
                  </p>
                </div>
              </div>
            </div>

            <Link
              to="/monitors/new"
              className="mt-6 flex items-center justify-center gap-2 w-full py-2.5 px-4 bg-sky-600 hover:bg-sky-500 text-white text-sm font-semibold rounded-lg shadow-lg shadow-sky-600/30 transition-all"
            >
              <PlusCircle className="w-4 h-4" />
              <span>Configure New Monitor</span>
            </Link>
          </div>
        </div>
      </main>
    </div>
  );
};
