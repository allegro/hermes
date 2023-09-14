import { createRetransmissionTask } from '@/api/hermes-client';
import { dispatchAxiosErrorNotification } from '@/utils/notification-utils';
import { useGlobalI18n } from '@/i18n';
import { useNotificationsStore } from '@/store/app-notifications/useAppNotifications';
import type { OfflineRetransmissionTask } from '@/api/offline-retransmission';

export interface UseOfflineRetransmission {
  retransmit: (task: OfflineRetransmissionTask) => Promise<boolean>;
}

export function useOfflineRetransmission(): UseOfflineRetransmission {
  const retransmit = async (
    task: OfflineRetransmissionTask,
  ): Promise<boolean> => {
    const notificationsStore = useNotificationsStore();
    try {
      await createRetransmissionTask(task);
      await notificationsStore.dispatchNotification({
        title: useGlobalI18n().t(
          'notifications.offlineRetransmission.create.success',
          { sourceTopic: task.sourceTopic, targetTopic: task.targetTopic },
        ),
        text: '',
        type: 'success',
      });
      return true;
    } catch (e: any) {
      dispatchAxiosErrorNotification(
        e,
        notificationsStore,
        useGlobalI18n().t(
          'notifications.offlineRetransmission.create.failure',
          { sourceTopic: task.sourceTopic, targetTopic: task.targetTopic },
        ),
      );
      return false;
    }
  };
  return {
    retransmit,
  };
}
