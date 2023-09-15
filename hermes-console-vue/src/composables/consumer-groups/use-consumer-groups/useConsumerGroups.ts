import { dispatchErrorNotification } from '@/utils/notification-utils';
import {
  fetchConsumerGroups as getConsumerGroups,
  moveSubscriptionOffsets,
} from '@/api/hermes-client';
import { ref } from 'vue';
import { useGlobalI18n } from '@/i18n';
import { useNotificationsStore } from '@/store/app-notifications/useAppNotifications';
import type { ConsumerGroup } from '@/api/consumer-group';
import type { Ref } from 'vue';

export interface UseConsumerGroups {
  consumerGroups: Ref<ConsumerGroup[] | undefined>;
  moveOffsets: () => void;
  loading: Ref<boolean>;
  error: Ref<UseConsumerGroupsErrors>;
}

export interface UseConsumerGroupsErrors {
  fetchConsumerGroups: Error | null;
}

export function useConsumerGroups(
  topicName: string,
  subscriptionName: string,
): UseConsumerGroups {
  const consumerGroups = ref<ConsumerGroup[]>();
  const error = ref<UseConsumerGroupsErrors>({
    fetchConsumerGroups: null,
  });
  const loading = ref(false);

  const fetchConsumerGroups = async () => {
    try {
      loading.value = true;
      consumerGroups.value = (
        await getConsumerGroups(topicName, subscriptionName)
      ).data;
    } catch (e) {
      error.value.fetchConsumerGroups = e as Error;
    } finally {
      loading.value = false;
    }
  };

  const moveOffsets = async () => {
    const notificationsStore = useNotificationsStore();
    try {
      await moveSubscriptionOffsets(topicName, subscriptionName);
      await notificationsStore.dispatchNotification({
        title: useGlobalI18n().t(
          'notifications.subscriptionOffsets.move.success',
          {
            subscriptionName,
          },
        ),
        text: '',
        type: 'success',
      });
    } catch (e: any) {
      dispatchErrorNotification(
        e,
        notificationsStore,
        useGlobalI18n().t('notifications.subscriptionOffsets.move.failure', {
          subscriptionName,
        }),
      );
    }
  };

  fetchConsumerGroups();

  return {
    consumerGroups,
    moveOffsets,
    loading,
    error,
  };
}
