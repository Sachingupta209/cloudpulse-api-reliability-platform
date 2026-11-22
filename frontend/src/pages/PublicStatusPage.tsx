import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import axios from 'axios';
import {
  ShieldCheck,
  CheckCircle2,
  AlertTriangle,
  XCircle,
  Clock,
  Activity,
  Layers,
  ArrowLeft,
  RefreshCw,
  Info,
} from 'lucide-react';
import type { PublicStatusPage as IPublicStatusPage, PublicSystemStatus } from '../types';
import { ThemeSelector } from '../components/common/ThemeSelector';

export const PublicStatusPage: React.FC = () => {
  const { slug } = useParams<{ slug: string }>();
  const [data, setData] = useState<IPublicStatusPage | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isRefreshing, setIsRefreshing] = useState(false);

  useEffect(() => {
    if (slug) {
      fetchPublicStatus(slug);
    }
  }, [slug]);

  const fetchPublicStatus = async (slugVal: string) => {
    try {
      setIsRefreshing(true);
      setError(null);
      // Direct axios call without auth interceptors to avoid login redirect
      const res = await axios.get(`/api/public/status/${slugVal}`, {
        headers: { Accept: 'application/json' },
      });
      setData(res.data.data);
    } catch (err: any) {
      if (err.response?.status === 404) {
        setError('This status page does not exist or is currently private.');
      } else if (err.response?.status === 429) {
        setError('Rate limit exceeded. Please wait a moment and refresh.');
      } else {
        setError('Unable to load status page. Please try again later.');
      }
    } finally {
      setLoading(false);
      setIsRefreshing(false);
    }
  };

  const getStatusBanner = (status: PublicSystemStatus) => {
    switch (status) {
      case 'OPERATIONAL':
        return {
          bg: 'bg-emerald-500/10 border-emerald-500/30 text-emerald-700 dark:text-emerald-400',
          badge: 'bg-emerald-500 text-white',
          icon: CheckCircle2,
          headline: 'All Systems Operational',
          desc: 'All monitored services are running within acceptable performance and reliability thresholds.',
        };
      case 'DEGRADED':
        return {
          bg: 'bg-amber-500/10 border-amber-500/30 text-amber-700 dark:text-amber-400',
          badge: 'bg-amber-500 text-white',
          icon: AlertTriangle,
          headline: 'Partial System Outage',
          desc: 'One or more services are experiencing elevated latency or degraded health.',
        };
      case 'OUTAGE':
        return {
          bg: 'bg-rose-500/10 border-rose-500/30 text-rose-700 dark:text-rose-400',
          badge: 'bg-rose-500 text-white',
          icon: XCircle,
          headline: 'Major System Outage',
          desc: 'Critical services are unreachable or failing health checks.',
        };
      case 'MAINTENANCE':
        return {
          bg: 'bg-sky-500/10 border-sky-500/30 text-sky-700 dark:text-sky-400',
          badge: 'bg-sky-500 text-white',
          icon: Clock,
          headline: 'System Under Maintenance',
          desc: 'Monitored services are currently paused for scheduled maintenance.',
        };
      case 'NO_DATA':
      default:
        return {
          bg: 'bg-slate-500/10 border-slate-500/30 text-slate-700 dark:text-slate-400',
          badge: 'bg-slate-500 text-white',
          icon: Info,
          headline: 'No Active Monitoring Data',
          desc: 'No health check observations are currently available for this service group.',
        };
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-slate-50 dark:bg-slate-950 flex items-center justify-center p-4">
        <div className="flex flex-col items-center gap-3">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-sky-600 dark:border-sky-400" />
          <p className="text-sm text-slate-500 dark:text-slate-400">Loading live status...</p>
        </div>
      </div>
    );
  }

  if (error || !data) {
    return (
      <div className="min-h-screen bg-slate-50 dark:bg-slate-950 flex flex-col items-center justify-center p-4">
        <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-8 max-w-md w-full text-center shadow-lg">
          <div className="w-12 h-12 rounded-full bg-slate-100 dark:bg-slate-800 flex items-center justify-center mx-auto mb-4 text-slate-400">
            <XCircle className="w-6 h-6" />
          </div>
          <h2 className="text-xl font-bold text-slate-900 dark:text-white mb-2">Status Page Unavailable</h2>
          <p className="text-sm text-slate-500 dark:text-slate-400 mb-6">{error}</p>
          <Link
            to="/dashboard"
            className="inline-flex items-center gap-2 text-sm font-medium text-sky-600 dark:text-sky-400 hover:underline"
          >
            <ArrowLeft className="w-4 h-4" /> Go to Dashboard
          </Link>
        </div>
      </div>
    );
  }

  const banner = getStatusBanner(data.overallStatus);
  const BannerIcon = banner.icon;

  return (
    <div className="min-h-screen bg-slate-50 dark:bg-slate-950 text-slate-900 dark:text-white transition-colors duration-200">
      {/* Top Navigation Bar */}
      <header className="border-b border-slate-200 dark:border-slate-800 bg-white/80 dark:bg-slate-900/80 backdrop-blur-md sticky top-0 z-30">
        <div className="max-w-4xl mx-auto px-4 sm:px-6 h-16 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 rounded-lg bg-sky-600 flex items-center justify-center text-white shadow-sm">
              <ShieldCheck className="w-4 h-4" />
            </div>
            <div>
              <span className="font-bold text-base tracking-tight text-slate-900 dark:text-white block">
                {data.title}
              </span>
              <span className="text-[11px] text-slate-500 dark:text-slate-400 font-medium block">
                CloudPulse Reliability Status
              </span>
            </div>
          </div>
          <div className="flex items-center gap-3">
            <button
              onClick={() => slug && fetchPublicStatus(slug)}
              disabled={isRefreshing}
              title="Refresh status"
              className="p-1.5 text-slate-400 hover:text-slate-700 dark:hover:text-slate-200 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
            >
              <RefreshCw className={`w-4 h-4 ${isRefreshing ? 'animate-spin' : ''}`} />
            </button>
            <ThemeSelector />
          </div>
        </div>
      </header>

      {/* Main Content Area */}
      <main className="max-w-4xl mx-auto px-4 sm:px-6 py-8 space-y-8">
        {/* Overall Status Banner */}
        <div className={`p-6 rounded-2xl border ${banner.bg} shadow-xs flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4`}>
          <div className="flex items-start gap-4">
            <div className={`w-10 h-10 rounded-xl ${banner.badge} flex items-center justify-center shrink-0 shadow-sm`}>
              <BannerIcon className="w-5 h-5 text-white" />
            </div>
            <div>
              <h2 className="text-xl font-bold tracking-tight text-slate-900 dark:text-white">
                {banner.headline}
              </h2>
              <p className="text-sm opacity-90 mt-0.5 text-slate-600 dark:text-slate-300">
                {banner.desc}
              </p>
            </div>
          </div>
          <span className="text-xs text-slate-500 dark:text-slate-400 shrink-0 self-end sm:self-center">
            Updated {new Date(data.updatedAt).toLocaleTimeString()}
          </span>
        </div>

        {/* Description If Available */}
        {data.description && (
          <p className="text-sm text-slate-600 dark:text-slate-400 leading-relaxed -mt-4">
            {data.description}
          </p>
        )}

        {/* Monitored Services List */}
        <section className="space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 flex items-center gap-2">
              <Layers className="w-4 h-4" />
              Services ({data.monitors.length})
            </h3>
            <span className="text-xs text-slate-400 dark:text-slate-500">
              Uptime calculated over valid checks (24h / 7d / 30d)
            </span>
          </div>

          <div className="bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-800 divide-y divide-slate-100 dark:divide-slate-800 shadow-xs overflow-hidden">
            {data.monitors.length === 0 ? (
              <div className="p-8 text-center text-sm text-slate-500 dark:text-slate-400">
                No published monitors on this status page.
              </div>
            ) : (
              data.monitors.map((m, idx) => (
                <div key={idx} className="p-5 flex flex-col md:flex-row md:items-center justify-between gap-4">
                  {/* Service Info */}
                  <div className="flex items-center gap-3">
                    <span
                      className={`w-2.5 h-2.5 rounded-full shrink-0 ${
                        m.status === 'OPERATIONAL'
                          ? 'bg-emerald-500'
                          : m.status === 'DOWN'
                          ? 'bg-rose-500'
                          : m.status === 'DEGRADED'
                          ? 'bg-amber-500'
                          : m.status === 'PAUSED'
                          ? 'bg-sky-500'
                          : 'bg-slate-400'
                      }`}
                    />
                    <div>
                      <h4 className="font-semibold text-sm text-slate-900 dark:text-white">
                        {m.name}
                      </h4>
                      <span className="text-xs text-slate-400 dark:text-slate-500 capitalize">
                        {m.status.toLowerCase()}
                      </span>
                    </div>
                  </div>

                  {/* Uptime & Latency Metrics */}
                  <div className="grid grid-cols-4 gap-4 text-right">
                    <div>
                      <span className="text-[11px] block text-slate-400 dark:text-slate-500">24 Hours</span>
                      <span className="text-xs font-semibold text-slate-700 dark:text-slate-300">
                        {m.uptime24h !== null ? `${m.uptime24h.toFixed(2)}%` : '—'}
                      </span>
                    </div>
                    <div>
                      <span className="text-[11px] block text-slate-400 dark:text-slate-500">7 Days</span>
                      <span className="text-xs font-semibold text-slate-700 dark:text-slate-300">
                        {m.uptime7d !== null ? `${m.uptime7d.toFixed(2)}%` : '—'}
                      </span>
                    </div>
                    <div>
                      <span className="text-[11px] block text-slate-400 dark:text-slate-500">30 Days</span>
                      <span className="text-xs font-semibold text-slate-700 dark:text-slate-300">
                        {m.uptime30d !== null ? `${m.uptime30d.toFixed(2)}%` : '—'}
                      </span>
                    </div>
                    <div>
                      <span className="text-[11px] block text-slate-400 dark:text-slate-500">Avg Latency</span>
                      <span className="text-xs font-semibold text-slate-700 dark:text-slate-300">
                        {m.averageLatencyMs !== null ? `${Math.round(m.averageLatencyMs)} ms` : '—'}
                      </span>
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>
        </section>

        {/* 30-Day Incident History Section */}
        <section className="space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 flex items-center gap-2">
              <Activity className="w-4 h-4" />
              Past 30 Days Incident History
            </h3>
            <span className="text-xs text-slate-400 dark:text-slate-500">
              {data.recentIncidents.length} {data.recentIncidents.length === 1 ? 'incident' : 'incidents'}
            </span>
          </div>

          {data.recentIncidents.length === 0 ? (
            <div className="bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-800 p-8 text-center text-sm text-slate-500 dark:text-slate-400">
              <CheckCircle2 className="w-6 h-6 text-emerald-500 mx-auto mb-2" />
              No incidents reported in the last 30 days. All systems running smoothly.
            </div>
          ) : (
            <div className="space-y-3">
              {data.recentIncidents.map((inc, idx) => (
                <div
                  key={idx}
                  className="bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-800 p-5 shadow-xs space-y-2"
                >
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <span className="text-xs font-bold text-sky-600 dark:text-sky-400 uppercase tracking-wider">
                        {inc.monitorName}
                      </span>
                      <h4 className="font-semibold text-sm text-slate-900 dark:text-white mt-0.5">
                        {inc.title}
                      </h4>
                    </div>
                    <span
                      className={`text-xs px-2.5 py-0.5 rounded-full font-medium shrink-0 ${
                        inc.status === 'RESOLVED'
                          ? 'bg-emerald-100 text-emerald-800 dark:bg-emerald-950/40 dark:text-emerald-300'
                          : 'bg-rose-100 text-rose-800 dark:bg-rose-950/40 dark:text-rose-300'
                      }`}
                    >
                      {inc.status}
                    </span>
                  </div>

                  <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-slate-500 dark:text-slate-400 pt-1">
                    <span>Started: {new Date(inc.startedAt).toLocaleString()}</span>
                    {inc.resolvedAt && (
                      <span>Resolved: {new Date(inc.resolvedAt).toLocaleString()}</span>
                    )}
                    {inc.durationMinutes !== undefined && (
                      <span className="font-medium text-slate-700 dark:text-slate-300">
                        Duration: {inc.durationMinutes} min
                      </span>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>

        {/* Footer */}
        <footer className="pt-8 pb-12 border-t border-slate-200 dark:border-slate-800 text-center text-xs text-slate-400 dark:text-slate-500">
          Powered by <span className="font-semibold text-slate-600 dark:text-slate-300">CloudPulse</span> — API Reliability & Smart Health Validation Platform
        </footer>
      </main>
    </div>
  );
};
