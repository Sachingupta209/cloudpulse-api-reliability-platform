export interface User {
  id: number;
  email: string;
  firstName: string;
  lastName?: string;
  role: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  user: User;
}

export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE' | 'HEAD';
export type MonitorStatus = 'ACTIVE' | 'PAUSED';
export type HealthStatus = 'HEALTHY' | 'UNHEALTHY' | 'UNKNOWN';
export type RuleType =
  | 'HTTP_STATUS'
  | 'RESPONSE_TIME'
  | 'JSON_SYNTAX'
  | 'JSON_PATH_EXISTS'
  | 'JSON_PATH_EQUALS'
  | 'JSON_PATH_NOT_EQUALS'
  | 'JSON_PATH_CONTAINS'
  | 'HEADER_EQUALS';

export interface ValidationRule {
  id?: number;
  ruleType: RuleType;
  targetExpression?: string;
  expectedValue?: string;
  errorMessage?: string;
  enabled: boolean;
}

export interface Monitor {
  id: number;
  name: string;
  url: string;
  httpMethod: HttpMethod;
  checkIntervalSeconds: number;
  timeoutMs: number;
  expectedStatusCode: number;
  headersJson?: string;
  requestBody?: string;
  status: MonitorStatus;
  healthStatus: HealthStatus;
  consecutiveFailures: number;
  lastCheckedAt?: string;
  createdAt: string;
  updatedAt: string;
  validationRules: ValidationRule[];
  uptimePercentage?: number;
  avgResponseTimeMs?: number;
}

export interface CheckResult {
  id: number;
  monitorId: number;
  status: 'SUCCESS' | 'FAILURE';
  httpStatusCode?: number;
  responseTimeMs: number;
  failureReason?: string;
  validationDetailsJson?: string;
  checkedAt: string;
  ipAddress?: string;
}

export interface Incident {
  id: number;
  monitorId: number;
  monitorName: string;
  monitorUrl: string;
  status: 'OPEN' | 'RESOLVED';
  title: string;
  rootCause?: string;
  startedAt: string;
  resolvedAt?: string;
  downtimeSeconds?: number;
  createdAt: string;
}

export interface ResponseTimeTrend {
  timestamp: string;
  avgResponseTimeMs: number;
  totalChecks: number;
  successfulChecks: number;
}

export interface DashboardSummary {
  totalMonitors: number;
  healthyMonitors: number;
  unhealthyMonitors: number;
  pausedMonitors: number;
  overallUptimePercentage: number;
  averageResponseTimeMs: number;
  totalChecks: number;
  failedChecks: number;
  activeIncidents: number;
  recentIncidents: Incident[];
  responseTimeTrends: ResponseTimeTrend[];
}

export type NotificationType =
  | 'SERVICE_UNHEALTHY'
  | 'SERVICE_RECOVERED'
  | 'INCIDENT_OPENED'
  | 'INCIDENT_RESOLVED'
  | 'TEST';

export type NotificationStatus = 'PENDING' | 'IN_FLIGHT' | 'SENT' | 'FAILED' | 'SKIPPED';

export interface NotificationPreference {
  id: number;
  userId: number;
  emailEnabled: boolean;
  notifyOnIncidentOpened: boolean;
  notifyOnIncidentResolved: boolean;
  notifyOnUnhealthy: boolean;
  notifyOnRecovery: boolean;
  notificationEmail?: string;
}

export interface NotificationHistoryItem {
  id: number;
  monitorId?: number;
  monitorName?: string;
  monitorUrl?: string;
  incidentId?: number;
  incidentTitle?: string;
  notificationType: NotificationType;
  recipient: string;
  subject: string;
  status: NotificationStatus;
  failureReason?: string;
  retryCount: number;
  sentAt: string;
  createdAt?: string;
}

export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
  timestamp: string;
}

export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export type PublicSystemStatus = 'OPERATIONAL' | 'DEGRADED' | 'OUTAGE' | 'MAINTENANCE' | 'NO_DATA';

export interface StatusPageMonitorSummary {
  monitorId: number;
  name: string;
  status: string;
  healthStatus: string;
  displayOrder: number;
}

export interface StatusPage {
  id: number;
  title: string;
  description?: string;
  slug: string;
  isPublished: boolean;
  monitors: StatusPageMonitorSummary[];
  createdAt: string;
  updatedAt: string;
}

export interface StatusPageRequest {
  title: string;
  description?: string;
  slug?: string;
  isPublished?: boolean;
  monitorIds: number[];
}

export interface PublicMonitor {
  name: string;
  status: 'OPERATIONAL' | 'DEGRADED' | 'DOWN' | 'PAUSED' | 'UNKNOWN';
  uptime24h: number | null;
  uptime7d: number | null;
  uptime30d: number | null;
  averageLatencyMs: number | null;
}

export interface PublicIncident {
  monitorName: string;
  title: string;
  status: string;
  startedAt: string;
  resolvedAt?: string;
  durationMinutes?: number;
}

export interface PublicStatusPage {
  title: string;
  description?: string;
  overallStatus: PublicSystemStatus;
  updatedAt: string;
  monitors: PublicMonitor[];
  recentIncidents: PublicIncident[];
}

export type AnomalyType = 'LATENCY_SPIKE' | 'FAILURE_BURST' | 'UPTIME_DEGRADATION';
export type AnomalySeverity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export interface Anomaly {
  id: number;
  monitorId: number;
  monitorName: string;
  monitorUrl: string;
  anomalyType: AnomalyType;
  severity: AnomalySeverity;
  observedValue: number;
  baselineValue: number;
  description: string;
  detectedAt: string;
  acknowledged: boolean;
  acknowledgedAt?: string;
}

export interface AdvancedAnalytics {
  overallUptimePercentage: number;
  failureRatePercentage: number;
  mttrMinutes: number | null;
  mtbfHours: number | null;
  avgLatencyMs: number | null;
  minLatencyMs: number | null;
  maxLatencyMs: number | null;
  p95LatencyMs: number | null;
  p99LatencyMs: number | null;
  totalIncidents: number;
  activeIncidents: number;
  totalAnomalies: number;
  unacknowledgedAnomalies: number;
  uptime24h: number | null;
  uptime7d: number | null;
  uptime30d: number | null;
  recentAnomalies: Anomaly[];
  trends: ResponseTimeTrend[];
}
