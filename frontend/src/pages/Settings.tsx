import React from 'react';
import { Link } from 'react-router-dom';
import {
  Bell,
  Clock,
  Cpu,
  Globe,
  Lock,
  Palette,
  Server,
  ShieldCheck,
  User,
} from 'lucide-react';
import { Navbar } from '../components/layout/Navbar';
import { ThemeSelector } from '../components/common/ThemeSelector';
import { useAuth } from '../context/AuthContext';

export const Settings: React.FC = () => {
  const { user } = useAuth();

  return (
    <div className="flex-1 flex flex-col pb-16">
      <Navbar title="Settings & Architecture" subtitle="Account details, appearance, security policies, and engine configuration" />

      <main className="px-8 mt-8 space-y-8 max-w-5xl">
        {/* Theme & Appearance Configuration */}
        <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-6 shadow-xl transition-colors duration-200">
          <div className="flex items-center gap-3 border-b border-slate-200 dark:border-slate-800 pb-4 mb-5">
            <div className="w-10 h-10 rounded-lg bg-sky-50 dark:bg-sky-600/20 text-sky-600 dark:text-sky-400 flex items-center justify-center">
              <Palette className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-slate-900 dark:text-white tracking-tight">Appearance &amp; Theme</h3>
              <p className="text-xs text-slate-500 dark:text-slate-400">Select preferred color scheme across dashboards, tables, and telemetry charts</p>
            </div>
          </div>

          <ThemeSelector variant="detailed" />
        </div>

        {/* User Account Card */}
        <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-6 shadow-xl transition-colors duration-200">
          <div className="flex items-center gap-3 border-b border-slate-200 dark:border-slate-800 pb-4 mb-5">
            <div className="w-10 h-10 rounded-lg bg-sky-50 dark:bg-sky-600/20 text-sky-600 dark:text-sky-400 flex items-center justify-center">
              <User className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-slate-900 dark:text-white tracking-tight">Account Profile</h3>
              <p className="text-xs text-slate-500 dark:text-slate-400">Authenticated account credentials</p>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
            <div>
              <label className="text-xs font-semibold text-slate-500 dark:text-slate-400 block mb-1">Full Name</label>
              <p className="font-semibold text-slate-800 dark:text-slate-200">
                {user?.firstName} {user?.lastName || ''}
              </p>
            </div>
            <div>
              <label className="text-xs font-semibold text-slate-500 dark:text-slate-400 block mb-1">Email Address</label>
              <p className="font-semibold text-slate-800 dark:text-slate-200 font-mono">{user?.email}</p>
            </div>
            <div>
              <label className="text-xs font-semibold text-slate-500 dark:text-slate-400 block mb-1">Role &amp; Authority</label>
              <span className="text-xs px-2.5 py-0.5 bg-sky-50 dark:bg-sky-500/10 text-sky-600 dark:text-sky-400 font-bold rounded border border-sky-200 dark:border-sky-500/30">
                {user?.role}
              </span>
            </div>
            <div>
              <label className="text-xs font-semibold text-slate-500 dark:text-slate-400 block mb-1">Data Isolation</label>
              <span className="text-xs text-emerald-600 dark:text-emerald-400 font-semibold">Strict Tenant Isolation Active</span>
            </div>
          </div>
        </div>

        {/* Email & Alert Notifications Link Card */}
        <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-6 shadow-xl transition-colors duration-200">
          <div className="flex items-center justify-between border-b border-slate-200 dark:border-slate-800 pb-4 mb-5">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-lg bg-sky-50 dark:bg-sky-600/20 text-sky-600 dark:text-sky-400 flex items-center justify-center">
                <Bell className="w-5 h-5" />
              </div>
              <div>
                <h3 className="text-base font-bold text-slate-900 dark:text-white tracking-tight">Email &amp; Alert Notifications</h3>
                <p className="text-xs text-slate-500 dark:text-slate-400">Configure alert triggers for incidents, service outages, and delivery destinations</p>
              </div>
            </div>
            <Link
              to="/notifications"
              className="px-4 py-2 text-xs font-semibold rounded-lg bg-sky-600 hover:bg-sky-700 text-white transition-colors"
            >
              Manage Alert Rules &rarr;
            </Link>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-3 text-xs">
            <div className="p-3 bg-slate-50 dark:bg-slate-950/60 rounded-lg border border-slate-200 dark:border-slate-800">
              <span className="font-semibold text-slate-900 dark:text-white block mb-0.5">Incident Alerts</span>
              <span className="text-slate-500 dark:text-slate-400">Instant notification when threshold is exceeded</span>
            </div>
            <div className="p-3 bg-slate-50 dark:bg-slate-950/60 rounded-lg border border-slate-200 dark:border-slate-800">
              <span className="font-semibold text-slate-900 dark:text-white block mb-0.5">Health State Changes</span>
              <span className="text-slate-500 dark:text-slate-400">Alerts on Healthy &harr; Unhealthy transitions</span>
            </div>
            <div className="p-3 bg-slate-50 dark:bg-slate-950/60 rounded-lg border border-slate-200 dark:border-slate-800">
              <span className="font-semibold text-slate-900 dark:text-white block mb-0.5">Idempotency &amp; Retries</span>
              <span className="text-slate-500 dark:text-slate-400">Duplicate prevention with automated retry</span>
            </div>
          </div>
        </div>

        {/* Security & SSRF Policy */}
        <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-6 shadow-xl transition-colors duration-200">
          <div className="flex items-center gap-3 border-b border-slate-200 dark:border-slate-800 pb-4 mb-5">
            <div className="w-10 h-10 rounded-lg bg-rose-50 dark:bg-rose-600/20 text-rose-600 dark:text-rose-400 flex items-center justify-center">
              <ShieldCheck className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-slate-900 dark:text-white tracking-tight">SSRF &amp; Security Policies</h3>
              <p className="text-xs text-slate-500 dark:text-slate-400">Network egress controls and anti-rebinding defenses</p>
            </div>
          </div>

          <div className="space-y-3 text-xs text-slate-700 dark:text-slate-300">
            <div className="p-3 bg-slate-50 dark:bg-slate-950/60 rounded-lg border border-slate-200 dark:border-slate-800 flex items-start gap-3">
              <Lock className="w-4 h-4 text-sky-600 dark:text-sky-400 shrink-0 mt-0.5" />
              <div>
                <span className="font-semibold text-slate-900 dark:text-white">Connection-Time IP Destination Verification</span>
                <p className="text-slate-500 dark:text-slate-400 mt-0.5">
                  Protects against DNS Rebinding attacks by resolving and re-validating the actual IP address of all socket connections.
                </p>
              </div>
            </div>

            <div className="p-3 bg-slate-50 dark:bg-slate-950/60 rounded-lg border border-slate-200 dark:border-slate-800 flex items-start gap-3">
              <Globe className="w-4 h-4 text-rose-600 dark:text-rose-400 shrink-0 mt-0.5" />
              <div>
                <span className="font-semibold text-slate-900 dark:text-white">Prohibited IP Ranges</span>
                <p className="text-slate-500 dark:text-slate-400 mt-0.5">
                  Loopback (127.0.0.0/8, ::1), RFC 1918 (10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16), Link-Local, and Cloud Metadata (169.254.169.254, metadata.google.internal).
                </p>
              </div>
            </div>

            <div className="p-3 bg-slate-50 dark:bg-slate-950/60 rounded-lg border border-slate-200 dark:border-slate-800 flex items-start gap-3">
              <Cpu className="w-4 h-4 text-amber-600 dark:text-amber-400 shrink-0 mt-0.5" />
              <div>
                <span className="font-semibold text-slate-900 dark:text-white">Safe Redirect Evaluation</span>
                <p className="text-slate-500 dark:text-slate-400 mt-0.5">
                  All HTTP redirects (301, 302, 307, 308) are captured and manually inspected. Any attempt to redirect to an internal IP is immediately blocked.
                </p>
              </div>
            </div>
          </div>
        </div>

        {/* Engine Timezone & Architecture Card */}
        <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-6 shadow-xl transition-colors duration-200">
          <div className="flex items-center gap-3 border-b border-slate-200 dark:border-slate-800 pb-4 mb-5">
            <div className="w-10 h-10 rounded-lg bg-emerald-50 dark:bg-emerald-600/20 text-emerald-600 dark:text-emerald-400 flex items-center justify-center">
              <Server className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-slate-900 dark:text-white tracking-tight">Timezone &amp; Concurrency Architecture</h3>
              <p className="text-xs text-slate-500 dark:text-slate-400">Deterministic scheduling and database timestamp policy</p>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs">
            <div className="p-3.5 bg-slate-50 dark:bg-slate-950 rounded-lg border border-slate-200 dark:border-slate-800">
              <div className="flex items-center gap-2 text-slate-900 dark:text-white font-semibold mb-1">
                <Clock className="w-4 h-4 text-sky-600 dark:text-sky-400" />
                <span>UTC Timestamp Policy</span>
              </div>
              <p className="text-slate-500 dark:text-slate-400">
                All application servers and PostgreSQL tables enforce strict UTC (<code className="text-sky-600 dark:text-sky-300">TIMESTAMPTZ</code>).
                No localized clock offsets are permitted in backend persistence.
              </p>
            </div>

            <div className="p-3.5 bg-slate-50 dark:bg-slate-950 rounded-lg border border-slate-200 dark:border-slate-800">
              <div className="flex items-center gap-2 text-slate-900 dark:text-white font-semibold mb-1">
                <Cpu className="w-4 h-4 text-sky-600 dark:text-sky-400" />
                <span>Bounded Worker Pool</span>
              </div>
              <p className="text-slate-500 dark:text-slate-400">
                Monitoring executor utilizes a bounded thread pool (Core: 10, Max: 30, Queue: 500) with concurrent execution guards to prevent overlapping probe runs.
              </p>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
};
