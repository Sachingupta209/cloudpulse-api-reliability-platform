import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  AlertOctagon,
  CheckCircle2,
  Clock,
  Layers,
} from 'lucide-react';
import api from '../api/client';
import { Navbar } from '../components/layout/Navbar';
import type { Incident } from '../types';

export const Incidents: React.FC = () => {
  const [incidents, setIncidents] = useState<Incident[]>([]);
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'OPEN' | 'RESOLVED'>('ALL');
  const [loading, setLoading] = useState(true);

  const fetchIncidents = async () => {
    try {
      const url = statusFilter === 'ALL' ? '/incidents?size=50' : `/incidents?status=${statusFilter}&size=50`;
      const res = await api.get(url);
      setIncidents(res.data.data.content);
    } catch (err) {
      console.error('Failed to load incidents', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchIncidents();
  }, [statusFilter]);

  return (
    <div className="flex-1 flex flex-col pb-16">
      <Navbar
        title="Incidents"
        subtitle="Automatic outage detection and root cause incident logs"
        onRefresh={fetchIncidents}
      />

      <main className="px-8 mt-8 space-y-6 max-w-7xl">
        {/* Filter Bar */}
        <div className="flex items-center gap-2 border-b border-slate-200 dark:border-slate-800 pb-4">
          {(['ALL', 'OPEN', 'RESOLVED'] as const).map((filter) => (
            <button
              key={filter}
              onClick={() => setStatusFilter(filter)}
              className={`px-3.5 py-1.5 text-xs font-semibold rounded-lg transition-colors ${
                statusFilter === filter
                  ? 'bg-sky-600 text-white shadow-md shadow-sky-600/30'
                  : 'bg-white dark:bg-slate-900 text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-200 border border-slate-200 dark:border-slate-800'
              }`}
            >
              {filter === 'ALL' ? 'All Incidents' : filter === 'OPEN' ? 'Active / Open' : 'Resolved'}
            </button>
          ))}
        </div>

        {/* List */}
        {loading ? (
          <div className="py-20 flex justify-center">
            <div className="w-8 h-8 border-4 border-sky-500 border-t-transparent rounded-full animate-spin"></div>
          </div>
        ) : incidents.length > 0 ? (
          <div className="space-y-4">
            {incidents.map((incident) => (
              <div
                key={incident.id}
                className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-5 shadow-lg flex flex-col md:flex-row md:items-center justify-between gap-4 transition-colors duration-200"
              >
                <div className="space-y-1.5">
                  <div className="flex items-center gap-2.5">
                    <span
                      className={`text-xs px-2.5 py-0.5 rounded-full font-semibold inline-flex items-center gap-1.5 ${
                        incident.status === 'OPEN'
                          ? 'bg-rose-500/10 text-rose-600 dark:text-rose-400 border border-rose-500/30'
                          : 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-500/30'
                      }`}
                    >
                      {incident.status === 'OPEN' ? (
                        <AlertOctagon className="w-3.5 h-3.5" />
                      ) : (
                        <CheckCircle2 className="w-3.5 h-3.5" />
                      )}
                      {incident.status}
                    </span>

                    <h4 className="text-base font-bold text-slate-900 dark:text-white tracking-tight">{incident.title}</h4>
                  </div>

                  <p className="text-xs text-rose-700 dark:text-rose-300 font-mono bg-rose-50 dark:bg-rose-950/20 px-3 py-1.5 rounded-md border border-rose-200 dark:border-rose-900/30 max-w-2xl">
                    {incident.rootCause || 'Root cause unidentified'}
                  </p>

                  <div className="flex items-center gap-4 text-xs text-slate-500 dark:text-slate-400 pt-1">
                    <span className="flex items-center gap-1">
                      <Layers className="w-3.5 h-3.5 text-slate-400 dark:text-slate-500" />
                      <Link
                        to={`/monitors/${incident.monitorId}`}
                        className="text-sky-600 dark:text-sky-400 hover:underline font-medium"
                      >
                        {incident.monitorName}
                      </Link>
                    </span>

                    <span className="flex items-center gap-1">
                      <Clock className="w-3.5 h-3.5 text-slate-400 dark:text-slate-500" />
                      Started: {new Date(incident.startedAt).toLocaleString()}
                    </span>

                    {incident.resolvedAt && (
                      <span className="text-emerald-600 dark:text-emerald-400 font-medium">
                        Resolved: {new Date(incident.resolvedAt).toLocaleString()}
                      </span>
                    )}
                  </div>
                </div>

                <div className="text-right shrink-0">
                  {incident.downtimeSeconds != null ? (
                    <div className="text-xs">
                      <span className="text-slate-500 dark:text-slate-400 block mb-0.5">Total Outage</span>
                      <span className="font-mono font-bold text-slate-800 dark:text-slate-200 bg-slate-100 dark:bg-slate-800 px-2 py-1 rounded">
                        {incident.downtimeSeconds} seconds
                      </span>
                    </div>
                  ) : (
                    <span className="text-xs font-semibold text-rose-600 dark:text-rose-400 animate-pulse bg-rose-500/10 px-2.5 py-1 rounded-full border border-rose-500/20">
                      Ongoing Outage
                    </span>
                  )}
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl py-16 px-6 text-center shadow-xl transition-colors duration-200">
            <CheckCircle2 className="w-12 h-12 text-emerald-500/50 mx-auto mb-3" />
            <h3 className="text-base font-semibold text-slate-900 dark:text-white">No incidents found</h3>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-1 max-w-sm mx-auto">
              Your systems have been consistently reporting healthy. Consecutive failure threshold is set to 3.
            </p>
          </div>
        )}
      </main>
    </div>
  );
};
