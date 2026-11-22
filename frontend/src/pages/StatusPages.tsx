import React, { useEffect, useState } from 'react';
import {
  Globe,
  Plus,
  ExternalLink,
  Copy,
  Check,
  Trash2,
  Edit2,
  AlertCircle,
  Eye,
  EyeOff,
  Layers,
} from 'lucide-react';
import api from '../api/client';
import type { Monitor, StatusPage, StatusPageRequest } from '../types';

export const StatusPages: React.FC = () => {
  const [statusPages, setStatusPages] = useState<StatusPage[]>([]);
  const [monitors, setMonitors] = useState<Monitor[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  // Modal State
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingPage, setEditingPage] = useState<StatusPage | null>(null);
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [slug, setSlug] = useState('');
  const [isPublished, setIsPublished] = useState(false);
  const [selectedMonitorIds, setSelectedMonitorIds] = useState<number[]>([]);
  const [copiedSlug, setCopiedSlug] = useState<string | null>(null);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      setLoading(true);
      setError(null);
      const [spRes, mRes] = await Promise.all([
        api.get('/status-pages'),
        api.get('/monitors'),
      ]);
      setStatusPages(spRes.data.data || []);
      setMonitors(mRes.data.data || []);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load status pages');
    } finally {
      setLoading(false);
    }
  };

  const openCreateModal = () => {
    setEditingPage(null);
    setTitle('');
    setDescription('');
    setSlug('');
    setIsPublished(false);
    setSelectedMonitorIds([]);
    setIsModalOpen(true);
  };

  const openEditModal = (sp: StatusPage) => {
    setEditingPage(sp);
    setTitle(sp.title);
    setDescription(sp.description || '');
    setSlug(sp.slug);
    setIsPublished(sp.isPublished);
    setSelectedMonitorIds(sp.monitors.map((m) => m.monitorId));
    setIsModalOpen(true);
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim()) {
      setError('Title is required');
      return;
    }

    try {
      setSaving(true);
      setError(null);
      const payload: StatusPageRequest = {
        title: title.trim(),
        description: description.trim() || undefined,
        slug: slug.trim() || undefined,
        isPublished,
        monitorIds: selectedMonitorIds,
      };

      if (editingPage) {
        await api.put(`/status-pages/${editingPage.id}`, payload);
        setSuccess('Status page updated successfully');
      } else {
        await api.post('/status-pages', payload);
        setSuccess('Status page created successfully');
      }

      setIsModalOpen(false);
      await fetchData();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to save status page');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm('Are you sure you want to delete this status page? This action cannot be undone.')) {
      return;
    }

    try {
      setError(null);
      await api.delete(`/status-pages/${id}`);
      setSuccess('Status page deleted successfully');
      await fetchData();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to delete status page');
    }
  };

  const handleCopyLink = (slugVal: string) => {
    const fullUrl = `${window.location.origin}/status/${slugVal}`;
    navigator.clipboard.writeText(fullUrl);
    setCopiedSlug(slugVal);
    setTimeout(() => setCopiedSlug(null), 2500);
  };

  const toggleMonitorSelection = (monitorId: number) => {
    setSelectedMonitorIds((prev) =>
      prev.includes(monitorId)
        ? prev.filter((id) => id !== monitorId)
        : [...prev, monitorId]
    );
  };

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-white flex items-center gap-2.5">
            <Globe className="w-6 h-6 text-sky-600 dark:text-sky-400" />
            Public Status Pages
          </h1>
          <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">
            Publish real-time service health, historical uptime, and incident updates to your users.
          </p>
        </div>
        <button
          onClick={openCreateModal}
          className="inline-flex items-center justify-center gap-2 px-4 py-2 text-sm font-medium text-white bg-sky-600 hover:bg-sky-700 dark:bg-sky-500 dark:hover:bg-sky-600 rounded-lg shadow-sm transition-colors"
        >
          <Plus className="w-4 h-4" />
          Create Status Page
        </button>
      </div>

      {/* Notifications / Alerts */}
      {error && (
        <div className="p-4 rounded-lg bg-rose-50 dark:bg-rose-950/40 border border-rose-200 dark:border-rose-900/50 flex items-start gap-3">
          <AlertCircle className="w-5 h-5 text-rose-600 dark:text-rose-400 shrink-0 mt-0.5" />
          <div className="text-sm text-rose-800 dark:text-rose-200">{error}</div>
        </div>
      )}

      {success && (
        <div className="p-4 rounded-lg bg-emerald-50 dark:bg-emerald-950/40 border border-emerald-200 dark:border-emerald-900/50 flex items-start gap-3">
          <Check className="w-5 h-5 text-emerald-600 dark:text-emerald-400 shrink-0 mt-0.5" />
          <div className="text-sm text-emerald-800 dark:text-emerald-200">{success}</div>
        </div>
      )}

      {/* Loading State */}
      {loading ? (
        <div className="flex items-center justify-center min-h-[300px]">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-sky-600 dark:border-sky-400" />
        </div>
      ) : statusPages.length === 0 ? (
        /* Empty State */
        <div className="bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-800 p-12 text-center">
          <div className="w-12 h-12 rounded-xl bg-sky-50 dark:bg-sky-950/50 border border-sky-100 dark:border-sky-900/40 flex items-center justify-center mx-auto mb-4 text-sky-600 dark:text-sky-400">
            <Globe className="w-6 h-6" />
          </div>
          <h3 className="text-lg font-semibold text-slate-900 dark:text-white">No status pages yet</h3>
          <p className="text-sm text-slate-500 dark:text-slate-400 max-w-md mx-auto mt-1 mb-6">
            Create your first public status page to share real-time health and transparency with your stakeholders.
          </p>
          <button
            onClick={openCreateModal}
            className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-sky-600 hover:bg-sky-700 dark:bg-sky-500 dark:hover:bg-sky-600 rounded-lg shadow-sm"
          >
            <Plus className="w-4 h-4" />
            Create Status Page
          </button>
        </div>
      ) : (
        /* Status Pages Grid */
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {statusPages.map((sp) => {
            return (
              <div
                key={sp.id}
                className="bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-800 p-6 flex flex-col justify-between shadow-xs transition-shadow hover:shadow-md"
              >
                <div>
                  <div className="flex items-center justify-between gap-2 mb-3">
                    <span
                      className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium ${
                        sp.isPublished
                          ? 'bg-emerald-50 text-emerald-700 border border-emerald-200 dark:bg-emerald-950/40 dark:text-emerald-400 dark:border-emerald-900/50'
                          : 'bg-slate-100 text-slate-600 border border-slate-200 dark:bg-slate-800 dark:text-slate-400 dark:border-slate-700'
                      }`}
                    >
                      {sp.isPublished ? (
                        <>
                          <Eye className="w-3.5 h-3.5" />
                          Published
                        </>
                      ) : (
                        <>
                          <EyeOff className="w-3.5 h-3.5" />
                          Private
                        </>
                      )}
                    </span>
                    <span className="text-xs text-slate-400 dark:text-slate-500 flex items-center gap-1">
                      <Layers className="w-3.5 h-3.5" />
                      {sp.monitors.length} {sp.monitors.length === 1 ? 'monitor' : 'monitors'}
                    </span>
                  </div>

                  <h3 className="text-lg font-bold text-slate-900 dark:text-white truncate">
                    {sp.title}
                  </h3>
                  {sp.description && (
                    <p className="text-sm text-slate-500 dark:text-slate-400 mt-1 line-clamp-2">
                      {sp.description}
                    </p>
                  )}

                  {/* Public Link Preview */}
                  <div className="mt-4 p-2.5 rounded-lg bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 text-xs">
                    <span className="text-slate-400 dark:text-slate-500 block mb-1">Public URL:</span>
                    <div className="flex items-center justify-between gap-2">
                      <span className="truncate text-sky-600 dark:text-sky-400 font-mono">
                        /status/{sp.slug}
                      </span>
                      <div className="flex items-center gap-1 shrink-0">
                        <button
                          onClick={() => handleCopyLink(sp.slug)}
                          title="Copy Public URL"
                          className="p-1 text-slate-400 hover:text-slate-700 dark:hover:text-slate-200 rounded transition-colors"
                        >
                          {copiedSlug === sp.slug ? (
                            <Check className="w-3.5 h-3.5 text-emerald-600" />
                          ) : (
                            <Copy className="w-3.5 h-3.5" />
                          )}
                        </button>
                        <a
                          href={`/status/${sp.slug}`}
                          target="_blank"
                          rel="noreferrer"
                          title="Open in new tab"
                          className="p-1 text-slate-400 hover:text-sky-600 dark:hover:text-sky-400 rounded transition-colors"
                        >
                          <ExternalLink className="w-3.5 h-3.5" />
                        </a>
                      </div>
                    </div>
                  </div>
                </div>

                {/* Card Actions */}
                <div className="mt-6 pt-4 border-t border-slate-100 dark:border-slate-800/80 flex items-center justify-between">
                  <a
                    href={`/status/${sp.slug}`}
                    target="_blank"
                    rel="noreferrer"
                    className="inline-flex items-center gap-1 text-xs font-medium text-sky-600 dark:text-sky-400 hover:underline"
                  >
                    View Page <ExternalLink className="w-3 h-3" />
                  </a>
                  <div className="flex items-center gap-2">
                    <button
                      onClick={() => openEditModal(sp)}
                      className="p-1.5 text-slate-400 hover:text-slate-700 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-md transition-colors"
                      title="Edit Status Page"
                    >
                      <Edit2 className="w-4 h-4" />
                    </button>
                    <button
                      onClick={() => handleDelete(sp.id)}
                      className="p-1.5 text-slate-400 hover:text-rose-600 dark:hover:text-rose-400 hover:bg-rose-50 dark:hover:bg-rose-950/40 rounded-md transition-colors"
                      title="Delete Status Page"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Create / Edit Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-xs">
          <div className="bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-800 shadow-xl max-w-lg w-full max-h-[90vh] flex flex-col">
            <div className="p-6 border-b border-slate-100 dark:border-slate-800 flex items-center justify-between">
              <h2 className="text-lg font-bold text-slate-900 dark:text-white flex items-center gap-2">
                <Globe className="w-5 h-5 text-sky-600 dark:text-sky-400" />
                {editingPage ? 'Edit Status Page' : 'Create Status Page'}
              </h2>
              <button
                onClick={() => setIsModalOpen(false)}
                className="text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 text-sm font-semibold"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleSave} className="p-6 space-y-4 overflow-y-auto flex-1">
              <div>
                <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 uppercase tracking-wider mb-1.5">
                  Page Title *
                </label>
                <input
                  type="text"
                  required
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  placeholder="e.g. Acme Services Status"
                  className="w-full px-3.5 py-2 text-sm rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white focus:outline-hidden focus:ring-2 focus:ring-sky-500"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 uppercase tracking-wider mb-1.5">
                  Description
                </label>
                <textarea
                  rows={2}
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder="Brief description visible to visitors"
                  className="w-full px-3.5 py-2 text-sm rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white focus:outline-hidden focus:ring-2 focus:ring-sky-500"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 uppercase tracking-wider mb-1.5">
                  Custom Slug (Optional)
                </label>
                <input
                  type="text"
                  value={slug}
                  onChange={(e) => setSlug(e.target.value)}
                  placeholder="e.g. acme-api (leave blank to auto-generate)"
                  className="w-full px-3.5 py-2 text-sm rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white font-mono focus:outline-hidden focus:ring-2 focus:ring-sky-500"
                />
                <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
                  Leave empty to automatically assign a secure, random non-guessable slug.
                </p>
              </div>

              {/* Publish Toggle */}
              <div className="flex items-center justify-between p-3.5 rounded-lg border border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-950">
                <div>
                  <span className="text-sm font-semibold text-slate-900 dark:text-white block">
                    Public Visibility
                  </span>
                  <span className="text-xs text-slate-500 dark:text-slate-400">
                    Make this status page publicly accessible without login. Defaults to private.
                  </span>
                </div>
                <input
                  type="checkbox"
                  checked={isPublished}
                  onChange={(e) => setIsPublished(e.target.checked)}
                  className="h-5 w-5 rounded border-slate-300 text-sky-600 focus:ring-sky-500 cursor-pointer"
                />
              </div>

              {/* Monitor Selection Checklist */}
              <div>
                <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 uppercase tracking-wider mb-2">
                  Select Monitors to Display ({selectedMonitorIds.length} selected)
                </label>
                {monitors.length === 0 ? (
                  <p className="text-xs text-slate-500 dark:text-slate-400 italic">
                    No monitors available. Create a monitor first.
                  </p>
                ) : (
                  <div className="max-h-48 overflow-y-auto space-y-2 border border-slate-200 dark:border-slate-800 rounded-lg p-2 bg-slate-50/50 dark:bg-slate-950/50">
                    {monitors.map((m) => {
                      const isSelected = selectedMonitorIds.includes(m.id);
                      return (
                        <label
                          key={m.id}
                          className={`flex items-center justify-between p-2 rounded-md text-sm cursor-pointer transition-colors ${
                            isSelected
                              ? 'bg-sky-50 dark:bg-sky-950/40 border border-sky-200 dark:border-sky-900/50 text-sky-900 dark:text-sky-200'
                              : 'hover:bg-slate-100 dark:hover:bg-slate-800 text-slate-700 dark:text-slate-300'
                          }`}
                        >
                          <div className="flex items-center gap-2.5 truncate mr-2">
                            <input
                              type="checkbox"
                              checked={isSelected}
                              onChange={() => toggleMonitorSelection(m.id)}
                              className="h-4 w-4 rounded border-slate-300 text-sky-600 focus:ring-sky-500"
                            />
                            <span className="font-medium truncate">{m.name}</span>
                          </div>
                          <span
                            className={`text-xs px-2 py-0.5 rounded-full shrink-0 font-medium ${
                              m.healthStatus === 'HEALTHY'
                                ? 'bg-emerald-100 text-emerald-800 dark:bg-emerald-950/50 dark:text-emerald-300'
                                : m.healthStatus === 'UNHEALTHY'
                                ? 'bg-rose-100 text-rose-800 dark:bg-rose-950/50 dark:text-rose-300'
                                : 'bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-300'
                            }`}
                          >
                            {m.healthStatus}
                          </span>
                        </label>
                      );
                    })}
                  </div>
                )}
              </div>

              <div className="pt-4 border-t border-slate-100 dark:border-slate-800 flex items-center justify-end gap-3">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="px-4 py-2 text-sm font-medium text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={saving}
                  className="px-4 py-2 text-sm font-medium text-white bg-sky-600 hover:bg-sky-700 dark:bg-sky-500 dark:hover:bg-sky-600 rounded-lg transition-colors disabled:opacity-50"
                >
                  {saving ? 'Saving...' : editingPage ? 'Update Status Page' : 'Create Status Page'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
