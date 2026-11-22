import api from './client';
import type { ApiResponse, NotificationHistoryItem, NotificationPreference, PagedResponse } from '../types';

export interface UpdatePreferencesRequest {
  emailEnabled: boolean;
  notifyOnIncidentOpened: boolean;
  notifyOnIncidentResolved: boolean;
  notifyOnUnhealthy: boolean;
  notifyOnRecovery: boolean;
  notificationEmail?: string;
}

export const notificationsApi = {
  getPreferences: async () => {
    const response = await api.get<ApiResponse<NotificationPreference>>('/notifications/preferences');
    return response.data;
  },

  updatePreferences: async (data: UpdatePreferencesRequest) => {
    const response = await api.put<ApiResponse<NotificationPreference>>('/notifications/preferences', data);
    return response.data;
  },

  getHistory: async (page = 0, size = 20) => {
    const response = await api.get<ApiResponse<PagedResponse<NotificationHistoryItem>>>(
      `/notifications/history?page=${page}&size=${size}`
    );
    return response.data;
  },

  sendTestNotification: async () => {
    const response = await api.post<ApiResponse<NotificationHistoryItem>>('/notifications/test');
    return response.data;
  },
};
