import { v4 as generateUUID } from 'uuid';
import { ref } from 'vue';
import type {
  Notification,
  NotificationConfig,
  NotificationsState,
} from '@/store/app-notifications/types';

export interface UseAppNotifications {
  dispatchNotification: (
    notificationConfig: NotificationConfig,
  ) => Promise<void>;
  removeNotification: (id: string) => Promise<void>;
  notifications: Notification[];
}

const state = ref<NotificationsState>({
  notifications: [],
});

export const useAppNotifications = (): UseAppNotifications => {
  async function dispatchNotification({
    title,
    text,
    type,
  }: NotificationConfig): Promise<void> {
    const notification: Notification = {
      id: generateUUID(),
      title,
      text,
      type,
    };
    state.value.notifications.push(notification);
  }
  async function removeNotification(id: string): Promise<void> {
    state.value.notifications = state.value.notifications.filter(
      (notification) => notification.id !== id,
    );
  }
  const notifications = state.value.notifications;

  return {
    dispatchNotification,
    removeNotification,
    notifications,
  };
};
