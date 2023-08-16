import { v4 as generateUUID } from 'uuid';
import { ref } from 'vue';
import type {
  Notification,
  NotificationConfig,
  NotificationsState,
} from '@/store/app-notifications/types';

export interface UseAppNotifications {
  dispatchNotification: (notificationConfig: NotificationConfig) => void;
  notifications: Notification[];
}

const state = ref<NotificationsState>({
  notifications: [],
});

export const useAppNotifications = (): UseAppNotifications => {
  const dispatchNotification = ({ title, text, type }: NotificationConfig) => {
    const notification: Notification = {
      id: generateUUID(),
      title,
      text,
      type,
    };
    state.value.notifications.push(notification);
  };
  const notifications = state.value.notifications;

  return {
    dispatchNotification,
    notifications,
  };
};
