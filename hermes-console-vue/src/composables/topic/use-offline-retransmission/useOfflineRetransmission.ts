import { createRetransmissionTask } from '@/api/hermes-client';
import { useGlobalI18n } from '@/i18n';
import { useNotificationsStore } from '@/store/app-notifications/useAppNotifications';
import type { OfflineRetransmissionTask } from '@/api/offline-retransmission';

export interface UseOfflineRetransmission {
  retransmit: (task: OfflineRetransmissionTask) => void;
}

export function useOfflineRetransmission(): UseOfflineRetransmission {
  const retransmit = async (task: OfflineRetransmissionTask) => {
    const notificationsStore = useNotificationsStore();
    try {
      await createRetransmissionTask(task);
      await notificationsStore.dispatchNotification({
        title: useGlobalI18n().t(
          'notifications.offlineRetransmission.create.success',
          { sourceTopic: task.sourceTopic, targetTopic: task.targetTopic },
        ),
        text: '',
        type: 'error',
      });
    } catch (e) {
      await notificationsStore.dispatchNotification({
        title: useGlobalI18n().t(
          'notifications.offlineRetransmission.create.failure',
          { sourceTopic: task.sourceTopic, targetTopic: task.targetTopic },
        ),
        text: (e as Error).message,
        type: 'error',
      });
    }
  };
  return {
    retransmit,
  };
}
