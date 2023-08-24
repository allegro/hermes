import { defineStore } from 'pinia';
import { v4 as generateUUID } from 'uuid';
import type {
  Notification,
  NotificationConfig,
  NotificationsState,
} from '@/store/app-notifications/types';

export const useNotificationsStore = defineStore('notifications', {
  state: (): NotificationsState => {
    return {
      notifications: [],
    };
  },
  actions: {
    async dispatchNotification({ title, text, type }: NotificationConfig) {
      const notification: Notification = {
        id: generateUUID(),
        title,
        text,
        type,
      };
      this.notifications.push(notification);
    },
    async removeNotification(id: string) {
      this.notifications = this.notifications.filter(
        (notification) => notification.id !== id,
      );
    },
  },
});
