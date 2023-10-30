export interface NotificationConfig {
  title?: string;
  text: string;
  duration?: number;
  persistent?: boolean;
  type: NotificationType;
}

export type NotificationType = 'success' | 'info' | 'warning' | 'error';

export interface Notification {
  id: string;
  title?: string;
  text: string;
  type: NotificationType;
  duration?: number;
  persistent?: boolean;
}

export interface NotificationsState {
  notifications: Notification[];
}
