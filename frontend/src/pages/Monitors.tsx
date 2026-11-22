import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  CheckCircle2,
  Edit3,
  ExternalLink,
  Pause,
  Play,
  PlusCircle,
  RefreshCw,
  Search,
  Trash2,
} from 'lucide-react';
import api from '../api/client';
import { Navbar } from '../components/layout/Navbar';
import type { Monitor } from '../types';

export const Monitors: React.FC = () => {
  const [monitors, setMonitors] = useState<Monitor[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [checkingId, setCheckingId] = useState<number | null>(null);

  const fetchMonitors = async () => {
    try {
      const res = await api.get('/monitors');
      setMonitors(res.data.data);
    } catch (err) {
      console.error('Failed to fetch monitors', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchMonitors();
  }, []);

  const handleManualCheck = async (id: number) => {
    try {
      setCheckingId(id);
      await api.post(`/monitors/${id}/check`);
      await fetchMonitors();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Manual health check failed');
    } finally {
      setCheckingId(null);
    }
  };

  const handleTogglePause = async (id: number, currentStatus: string) => {
    try {
      if (currentStatus === 'ACTIVE') {
        await api.patch(`/monitors/${id}/pause`);
      } else {
        await api.patch(`/monitors/${id}/resume`);
      }
      await fetchMonitors();
    } catch (err) {
      console.error('Failed to toggle monitor status', err);
    }
  };

  const handleDelete = async (id: number, name: string) => {
    if (confirm(`Are you sure you want to permanently delete monitor "${name}"?`)) {
      try {
        await api.delete(`/monitors/${id}`);
        setMonitors((prev) => prev.filter((m) => m.id !== id));
      } catch (err) {
        console.error('Failed to delete monitor', err);
      }
    }
  };

  const filtered = monitors.filter((m) =>
    m.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
    m.url.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="flex-1 flex flex-col pb-12">
      <Navbar
        title="Monitors"
        subtitle="Manage and configure your API and website endpoints"
        onRefresh={fetchMonitors}
      />

      <main className="px-8 mt-8 space-y-6 max-w-7xl">
        {/* Header Action Row */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="relative flex-1 max-w-md">
            <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-3" />
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="Search monitors by name or URL..."
              className="w-full pl-10 pr-4 py-2 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-lg text-sm text-slate-900 dark:text-white placeholder-slate-400 dark:placeholder-slate-500 focus:outline-none focus:border-sky-500 transition-colors"
            />
          </div>

          <Link
            to="/monitors/new"
            className="flex items-center gap-2 px-4 py-2 bg-sky-600 hover:bg-sky-500 text-white text-sm font-semibold rounded-lg shadow-lg shadow-sky-600/30 transition-all shrink-0"
          >
            <PlusCircle className="w-4 h-4" />
            <span>Add Monitor</span>
          </Link>
        </div>

        {/* Table / List */}
        {loading ? (
          <div className="py-20 flex justify-center">
            <div className="w-8 h-8 border-4 border-sky-500 border-t-transparent rounded-full animate-spin"></div>
          </div>
        ) : filtered.length > 0 ? (
          <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl overflow-hidden shadow-xl transition-colors duration-200">
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="border-b border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-950/40 text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">
                    <th className="py-3.5 px-6">Monitor</th>
                    <th className="py-3.5 px-4">Status</th>
                    <th className="py-3.5 px-4">Uptime (24h)</th>
                    <th className="py-3.5 px-4">Avg Latency</th>
                    <th className="py-3.5 px-4">Interval</th>
                    <th className="py-3.5 px-6 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-200 dark:divide-slate-800/80 text-sm">
                  {filtered.map((monitor) => (
                    <tr key={monitor.id} className="hover:bg-slate-50 dark:hover:bg-slate-800/30 transition-colors">
                      <td className="py-4 px-6">
                        <div className="flex items-center gap-3">
                          <span
                            className={`w-2.5 h-2.5 rounded-full shrink-0 ${
                              monitor.status === 'PAUSED'
                                ? 'bg-amber-400'
                                : monitor.healthStatus === 'HEALTHY'
                                ? 'bg-emerald-500 shadow-sm shadow-emerald-400'
                                : monitor.healthStatus === 'UNHEALTHY'
                                ? 'bg-rose-500 shadow-sm shadow-rose-500'
                                : 'bg-slate-400'
                            }`}
                          />
                          <div>
                            <Link
                              to={`/monitors/${monitor.id}`}
                              className="font-semibold text-slate-900 dark:text-slate-100 hover:text-sky-600 dark:hover:text-sky-400 transition-colors block"
                            >
                              {monitor.name}
                            </Link>
                            <span className="text-xs text-slate-500 dark:text-slate-400 font-mono flex items-center gap-1.5 mt-0.5 truncate max-w-xs">
                              <span className="px-1.5 py-0.2 bg-slate-100 dark:bg-slate-800 rounded text-[10px] font-bold text-sky-700 dark:text-sky-400">
                                {monitor.httpMethod}
                              </span>
                              {monitor.url}
                            </span>
                          </div>
                        </div>
                      </td>

                      <td className="py-4 px-4">
                        <span
                          className={`text-xs px-2.5 py-1 rounded-full font-semibold inline-flex items-center gap-1.5 ${
                            monitor.status === 'PAUSED'
                              ? 'bg-amber-500/10 text-amber-600 dark:text-amber-400 border border-amber-500/30'
                              : monitor.healthStatus === 'HEALTHY'
                              ? 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-500/30'
                              : monitor.healthStatus === 'UNHEALTHY'
                              ? 'bg-rose-500/10 text-rose-600 dark:text-rose-400 border border-rose-500/30'
                              : 'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400'
                          }`}
                        >
                          {monitor.status === 'PAUSED' ? 'Paused' : monitor.healthStatus}
                        </span>
                      </td>

                      <td className="py-4 px-4 font-semibold text-slate-800 dark:text-slate-200">
                        {monitor.uptimePercentage != null ? `${monitor.uptimePercentage.toFixed(1)}%` : '100%'}
                      </td>

                      <td className="py-4 px-4 font-mono text-xs text-slate-700 dark:text-slate-300">
                        {monitor.avgResponseTimeMs != null ? `${Math.round(monitor.avgResponseTimeMs)} ms` : '—'}
                      </td>

                      <td className="py-4 px-4 text-xs text-slate-500 dark:text-slate-400">
                        {monitor.checkIntervalSeconds}s
                      </td>

                      <td className="py-4 px-6 text-right">
                        <div className="flex items-center justify-end gap-2">
                          <button
                            onClick={() => handleManualCheck(monitor.id)}
                            disabled={checkingId === monitor.id}
                            title="Execute health check now"
                            className="p-1.5 text-slate-400 hover:text-sky-600 dark:hover:text-sky-400 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg transition-colors disabled:opacity-50"
                          >
                            <RefreshCw
                              className={`w-4 h-4 ${checkingId === monitor.id ? 'animate-spin text-sky-500' : ''}`}
                            />
                          </button>

                          <button
                            onClick={() => handleTogglePause(monitor.id, monitor.status)}
                            title={monitor.status === 'ACTIVE' ? 'Pause monitor' : 'Resume monitor'}
                            className="p-1.5 text-slate-400 hover:text-amber-600 dark:hover:text-amber-400 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg transition-colors"
                          >
                            {monitor.status === 'ACTIVE' ? <Pause className="w-4 h-4" /> : <Play className="w-4 h-4" />}
                          </button>

                          <Link
                            to={`/monitors/${monitor.id}/edit`}
                            title="Edit monitor configuration"
                            className="p-1.5 text-slate-400 hover:text-sky-600 dark:hover:text-sky-400 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg transition-colors"
                          >
                            <Edit3 className="w-4 h-4" />
                          </Link>

                          <Link
                            to={`/monitors/${monitor.id}`}
                            title="View monitor details"
                            className="p-1.5 text-slate-400 hover:text-slate-800 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg transition-colors"
                          >
                            <ExternalLink className="w-4 h-4" />
                          </Link>

                          <button
                            onClick={() => handleDelete(monitor.id, monitor.name)}
                            title="Delete monitor"
                            className="p-1.5 text-slate-400 hover:text-rose-600 dark:hover:text-rose-400 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg transition-colors"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        ) : (
          <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl py-16 px-6 text-center shadow-xl transition-colors duration-200">
            <CheckCircle2 className="w-12 h-12 text-slate-400 dark:text-slate-600 mx-auto mb-3" />
            <h3 className="text-base font-semibold text-slate-900 dark:text-white">No monitors configured yet</h3>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-1 max-w-sm mx-auto">
              Add your first API endpoint or website to initiate automated health checks and smart JSON assertions.
            </p>
            <Link
              to="/monitors/new"
              className="mt-5 inline-flex items-center gap-2 px-4 py-2 bg-sky-600 hover:bg-sky-500 text-white text-xs font-semibold rounded-lg shadow-lg shadow-sky-600/30 transition-all"
            >
              <PlusCircle className="w-4 h-4" />
              <span>Create Monitor</span>
            </Link>
          </div>
        )}
      </main>
    </div>
  );
};
