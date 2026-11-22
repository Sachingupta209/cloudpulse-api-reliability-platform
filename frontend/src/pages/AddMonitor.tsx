import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  AlertCircle,
  ArrowLeft,
  Clock,
  Code2,
  FileText,
  Plus,
  Trash2,
} from 'lucide-react';
import api from '../api/client';
import { Navbar } from '../components/layout/Navbar';
import type { HttpMethod, RuleType, ValidationRule } from '../types';

export const AddMonitor: React.FC = () => {
  const navigate = useNavigate();

  const [name, setName] = useState('');
  const [url, setUrl] = useState('');
  const [httpMethod, setHttpMethod] = useState<HttpMethod>('GET');
  const [checkIntervalSeconds, setCheckIntervalSeconds] = useState(60);
  const [timeoutMs, setTimeoutMs] = useState(10000);
  const [expectedStatusCode, setExpectedStatusCode] = useState(200);
  const [headersJson, setHeadersJson] = useState('');
  const [requestBody, setRequestBody] = useState('');

  const [validationRules, setValidationRules] = useState<ValidationRule[]>([
    {
      ruleType: 'JSON_PATH_EQUALS',
      targetExpression: '$.status',
      expectedValue: 'success',
      errorMessage: 'API returned application-level failure',
      enabled: true,
    },
  ]);

  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const handleAddRule = (type: RuleType = 'JSON_PATH_EQUALS') => {
    let target = '$.status';
    let expected = 'success';
    let errorMsg = '';

    if (type === 'RESPONSE_TIME') {
      target = 'responseTimeMs';
      expected = '2000';
      errorMsg = 'Response latency SLA exceeded 2000ms';
    } else if (type === 'HEADER_EQUALS') {
      target = 'Content-Type';
      expected = 'application/json';
      errorMsg = 'Expected header Content-Type to equal application/json';
    } else if (type === 'JSON_SYNTAX') {
      target = 'body';
      expected = 'valid_json';
      errorMsg = 'Response payload is not valid JSON';
    } else if (type === 'JSON_PATH_EXISTS') {
      target = '$.data';
      expected = '';
      errorMsg = 'Expected field $.data not found in JSON response';
    }

    setValidationRules([
      ...validationRules,
      {
        ruleType: type,
        targetExpression: target,
        expectedValue: expected,
        errorMessage: errorMsg,
        enabled: true,
      },
    ]);
  };

  const handleRemoveRule = (index: number) => {
    setValidationRules(validationRules.filter((_, i) => i !== index));
  };

  const handleRuleChange = (index: number, field: keyof ValidationRule, value: any) => {
    const updated = [...validationRules];
    updated[index] = { ...updated[index], [field]: value };
    setValidationRules(updated);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSubmitting(true);

    try {
      await api.post('/monitors', {
        name,
        url,
        httpMethod,
        checkIntervalSeconds: Number(checkIntervalSeconds),
        timeoutMs: Number(timeoutMs),
        expectedStatusCode: Number(expectedStatusCode),
        headersJson: headersJson.trim() ? headersJson : null,
        requestBody: requestBody.trim() ? requestBody : null,
        validationRules,
      });

      navigate('/monitors');
    } catch (err: any) {
      setError(
        err.response?.data?.message ||
        err.response?.data?.error ||
        'Failed to configure monitor. Check URL syntax and SSRF rules.'
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="flex-1 flex flex-col pb-16">
      <Navbar title="Create Monitor" subtitle="Configure proactive health checks and JSON assertions" />

      <main className="px-4 sm:px-8 mt-6 sm:mt-8 max-w-4xl mx-auto w-full">
        <button
          onClick={() => navigate('/monitors')}
          className="flex items-center gap-2 text-xs font-semibold text-slate-500 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-200 mb-6 transition-colors"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Back to monitors list</span>
        </button>

        {error && (
          <div className="mb-6 p-4 bg-rose-500/10 border border-rose-500/30 rounded-xl flex items-start gap-3 text-rose-600 dark:text-rose-400 text-sm">
            <AlertCircle className="w-5 h-5 shrink-0 mt-0.5" />
            <div>
              <p className="font-semibold">Configuration Error</p>
              <p className="text-xs text-rose-700 dark:text-rose-300 mt-0.5">{error}</p>
            </div>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-8">
          {/* Section 1: Basic Target Details */}
          <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-5 sm:p-6 shadow-xl space-y-5 transition-colors duration-200">
            <div className="border-b border-slate-200 dark:border-slate-800 pb-3">
              <h3 className="text-base font-bold text-slate-900 dark:text-white tracking-tight">Endpoint Details</h3>
              <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">Specify the target URL, HTTP method, and network timeouts</p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div className="md:col-span-2">
                <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 uppercase tracking-wider mb-2">
                  Monitor Name
                </label>
                <input
                  type="text"
                  required
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="e.g. Production Billing API"
                  className="w-full px-3.5 py-2.5 bg-white dark:bg-slate-950 border border-slate-300 dark:border-slate-800 rounded-lg text-sm text-slate-900 dark:text-white placeholder-slate-400 dark:placeholder-slate-500 focus:outline-none focus:border-sky-500 transition-colors"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 uppercase tracking-wider mb-2">
                  HTTP Method
                </label>
                <select
                  value={httpMethod}
                  onChange={(e) => setHttpMethod(e.target.value as HttpMethod)}
                  className="w-full px-3.5 py-2.5 bg-white dark:bg-slate-950 border border-slate-300 dark:border-slate-800 rounded-lg text-sm text-slate-900 dark:text-white focus:outline-none focus:border-sky-500 transition-colors"
                >
                  <option value="GET">GET</option>
                  <option value="POST">POST</option>
                  <option value="PUT">PUT</option>
                  <option value="PATCH">PATCH</option>
                  <option value="DELETE">DELETE</option>
                  <option value="HEAD">HEAD</option>
                </select>
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 uppercase tracking-wider mb-2">
                Target URL
              </label>
              <input
                type="url"
                required
                value={url}
                onChange={(e) => setUrl(e.target.value)}
                placeholder="https://api.yourdomain.com/v1/health"
                className="w-full px-3.5 py-2.5 bg-white dark:bg-slate-950 border border-slate-300 dark:border-slate-800 rounded-lg text-sm text-slate-900 dark:text-white placeholder-slate-400 dark:placeholder-slate-500 font-mono focus:outline-none focus:border-sky-500 transition-colors"
              />
              <p className="text-[11px] text-slate-500 dark:text-slate-400 mt-1.5">
                SSRF Protection active: localhost, 127.0.0.1, 10.x.x.x, 192.168.x.x, and cloud metadata IPs (169.254.169.254) are rejected.
              </p>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-4 pt-2">
              <div>
                <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 uppercase tracking-wider mb-2">
                  Check Interval (Seconds)
                </label>
                <input
                  type="number"
                  min={10}
                  max={86400}
                  value={checkIntervalSeconds}
                  onChange={(e) => setCheckIntervalSeconds(Number(e.target.value))}
                  className="w-full px-3.5 py-2 bg-white dark:bg-slate-950 border border-slate-300 dark:border-slate-800 rounded-lg text-sm text-slate-900 dark:text-white focus:outline-none focus:border-sky-500 transition-colors"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 uppercase tracking-wider mb-2">
                  Timeout (ms)
                </label>
                <input
                  type="number"
                  min={1000}
                  max={30000}
                  value={timeoutMs}
                  onChange={(e) => setTimeoutMs(Number(e.target.value))}
                  className="w-full px-3.5 py-2 bg-white dark:bg-slate-950 border border-slate-300 dark:border-slate-800 rounded-lg text-sm text-slate-900 dark:text-white focus:outline-none focus:border-sky-500 transition-colors"
                />
              </div>

              <div className="sm:col-span-2 md:col-span-1">
                <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 uppercase tracking-wider mb-2">
                  Expected HTTP Code
                </label>
                <input
                  type="number"
                  min={100}
                  max={599}
                  value={expectedStatusCode}
                  onChange={(e) => setExpectedStatusCode(Number(e.target.value))}
                  className="w-full px-3.5 py-2 bg-white dark:bg-slate-950 border border-slate-300 dark:border-slate-800 rounded-lg text-sm text-slate-900 dark:text-white focus:outline-none focus:border-sky-500 transition-colors"
                />
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 pt-2">
              <div>
                <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 uppercase tracking-wider mb-2">
                  Custom Headers (JSON format)
                </label>
                <textarea
                  rows={2}
                  value={headersJson}
                  onChange={(e) => setHeadersJson(e.target.value)}
                  placeholder='{"Authorization": "Bearer token", "X-Custom": "val"}'
                  className="w-full px-3.5 py-2 bg-white dark:bg-slate-950 border border-slate-300 dark:border-slate-800 rounded-lg text-xs font-mono text-slate-900 dark:text-white placeholder-slate-400 dark:placeholder-slate-600 focus:outline-none focus:border-sky-500 transition-colors"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 uppercase tracking-wider mb-2">
                  Request Body (Optional)
                </label>
                <textarea
                  rows={2}
                  value={requestBody}
                  onChange={(e) => setRequestBody(e.target.value)}
                  placeholder='{"query": "{ ping }"}'
                  className="w-full px-3.5 py-2 bg-white dark:bg-slate-950 border border-slate-300 dark:border-slate-800 rounded-lg text-xs font-mono text-slate-900 dark:text-white placeholder-slate-400 dark:placeholder-slate-600 focus:outline-none focus:border-sky-500 transition-colors"
                />
              </div>
            </div>
          </div>

          {/* Section 2: Smart API Validation Rules */}
          <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-5 sm:p-6 shadow-xl space-y-5 transition-colors duration-200">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-200 dark:border-slate-800 pb-3">
              <div>
                <h3 className="text-base font-bold text-slate-900 dark:text-white tracking-tight flex items-center gap-2">
                  <Code2 className="w-5 h-5 text-sky-600 dark:text-sky-400" />
                  Smart API Validation Rules
                </h3>
                <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
                  Configure JSONPath assertions, latency constraints, and header rules
                </p>
              </div>

              {/* Quick Rule Creator Buttons */}
              <div className="flex items-center flex-wrap gap-2">
                <button
                  type="button"
                  onClick={() => handleAddRule('JSON_PATH_EQUALS')}
                  className="flex items-center gap-1 px-2.5 py-1.5 bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-sky-600 dark:text-sky-400 text-xs font-semibold rounded-lg border border-slate-200 dark:border-slate-700 transition-colors"
                  title="Add JSON path validation rule"
                >
                  <Plus className="w-3.5 h-3.5" />
                  <span>JSON Rule</span>
                </button>
                <button
                  type="button"
                  onClick={() => handleAddRule('HEADER_EQUALS')}
                  className="flex items-center gap-1 px-2.5 py-1.5 bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-emerald-600 dark:text-emerald-400 text-xs font-semibold rounded-lg border border-slate-200 dark:border-slate-700 transition-colors"
                  title="Add HTTP Response Header validation rule"
                >
                  <FileText className="w-3.5 h-3.5" />
                  <span>Header Rule</span>
                </button>
                <button
                  type="button"
                  onClick={() => handleAddRule('RESPONSE_TIME')}
                  className="flex items-center gap-1 px-2.5 py-1.5 bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-amber-600 dark:text-amber-400 text-xs font-semibold rounded-lg border border-slate-200 dark:border-slate-700 transition-colors"
                  title="Add maximum response latency SLA constraint"
                >
                  <Clock className="w-3.5 h-3.5" />
                  <span>Latency SLA</span>
                </button>
              </div>
            </div>

            {validationRules.length > 0 ? (
              <div className="space-y-4">
                {validationRules.map((rule, idx) => (
                  <div
                    key={idx}
                    className="p-4 bg-slate-50 dark:bg-slate-950/70 border border-slate-200 dark:border-slate-800 rounded-lg space-y-3 relative"
                  >
                    <div className="flex items-center justify-between gap-3">
                      <span className="text-xs font-bold text-sky-600 dark:text-sky-400 uppercase tracking-wider">
                        Rule #{idx + 1}
                      </span>
                      <button
                        type="button"
                        onClick={() => handleRemoveRule(idx)}
                        className="text-slate-400 hover:text-rose-600 dark:hover:text-rose-400 p-1 transition-colors"
                        title="Delete rule"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
                      <div>
                        <label className="block text-[11px] font-semibold text-slate-500 dark:text-slate-400 mb-1">
                          Rule Type
                        </label>
                        <select
                          value={rule.ruleType}
                          onChange={(e) => handleRuleChange(idx, 'ruleType', e.target.value as RuleType)}
                          className="w-full px-3 py-1.5 bg-white dark:bg-slate-900 border border-slate-300 dark:border-slate-800 rounded-md text-xs text-slate-900 dark:text-white focus:outline-none focus:border-sky-500"
                        >
                          <option value="JSON_PATH_EQUALS">JSONPath Equals</option>
                          <option value="JSON_PATH_EXISTS">JSONPath Exists</option>
                          <option value="JSON_PATH_NOT_EQUALS">JSONPath Not Equals</option>
                          <option value="JSON_PATH_CONTAINS">JSONPath Contains</option>
                          <option value="RESPONSE_TIME">Max Response Time (Latency SLA)</option>
                          <option value="JSON_SYNTAX">Valid JSON Syntax</option>
                          <option value="HEADER_EQUALS">Header Equals</option>
                          <option value="HTTP_STATUS">HTTP Status Match</option>
                        </select>
                      </div>

                      <div>
                        <label className="block text-[11px] font-semibold text-slate-500 dark:text-slate-400 mb-1">
                          {rule.ruleType === 'RESPONSE_TIME'
                            ? 'Target Metric'
                            : rule.ruleType === 'HEADER_EQUALS'
                            ? 'Header Name'
                            : 'Target Expression (Path/Header)'}
                        </label>
                        <input
                          type="text"
                          value={rule.targetExpression || ''}
                          onChange={(e) => handleRuleChange(idx, 'targetExpression', e.target.value)}
                          placeholder="$.status or $.data.user"
                          className="w-full px-3 py-1.5 bg-white dark:bg-slate-900 border border-slate-300 dark:border-slate-800 rounded-md text-xs text-slate-900 dark:text-white font-mono placeholder-slate-400 dark:placeholder-slate-600 focus:outline-none focus:border-sky-500"
                        />
                      </div>

                      <div>
                        <label className="block text-[11px] font-semibold text-slate-500 dark:text-slate-400 mb-1">
                          {rule.ruleType === 'RESPONSE_TIME' ? 'Max Allowed Latency (ms)' : 'Expected Value'}
                        </label>
                        <input
                          type="text"
                          value={rule.expectedValue || ''}
                          onChange={(e) => handleRuleChange(idx, 'expectedValue', e.target.value)}
                          placeholder="success"
                          className="w-full px-3 py-1.5 bg-white dark:bg-slate-900 border border-slate-300 dark:border-slate-800 rounded-md text-xs text-slate-900 dark:text-white placeholder-slate-400 dark:placeholder-slate-600 focus:outline-none focus:border-sky-500"
                        />
                      </div>
                    </div>

                    <div>
                      <label className="block text-[11px] font-semibold text-slate-500 dark:text-slate-400 mb-1">
                        Custom Failure Diagnostic Message
                      </label>
                      <input
                        type="text"
                        value={rule.errorMessage || ''}
                        onChange={(e) => handleRuleChange(idx, 'errorMessage', e.target.value)}
                        placeholder="e.g. API returned application-level failure"
                        className="w-full px-3 py-1.5 bg-white dark:bg-slate-900 border border-slate-300 dark:border-slate-800 rounded-md text-xs text-slate-900 dark:text-white placeholder-slate-400 dark:placeholder-slate-600 focus:outline-none focus:border-sky-500"
                      />
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <p className="text-xs text-slate-500 py-3 text-center">
                No custom validation rules added. Monitor will only verify default HTTP status code.
              </p>
            )}
          </div>

          {/* Submit Actions */}
          <div className="flex flex-col-reverse sm:flex-row items-stretch sm:items-center justify-end gap-3 pt-2">
            <button
              type="button"
              onClick={() => navigate('/monitors')}
              className="px-5 py-2.5 text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-200 text-sm font-semibold rounded-lg transition-colors text-center"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={submitting}
              className="px-6 py-2.5 bg-sky-600 hover:bg-sky-500 text-white text-sm font-semibold rounded-lg shadow-lg shadow-sky-600/30 transition-all disabled:opacity-50 text-center"
            >
              {submitting ? 'Creating Monitor...' : 'Save and Start Monitoring'}
            </button>
          </div>
        </form>
      </main>
    </div>
  );
};
