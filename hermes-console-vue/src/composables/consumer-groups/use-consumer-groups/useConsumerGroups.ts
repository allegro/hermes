import {
  fetchConsumerGroups as getConsumerGroups,
  moveSubscriptionOffsets,
} from '@/api/hermes-client';
import { ref } from 'vue';
import { useNotificationsStore } from '@/store/app-notifications/useAppNotifications';
import type { ConsumerGroup } from '@/api/consumer-group';
import type { Ref } from 'vue';

import i18n from '@/main';

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
        title: i18n.global.t('subscription.moveOffsets.success', {
          subscriptionName,
        }),
        text: '',
        type: 'success',
      });
    } catch (e) {
      await notificationsStore.dispatchNotification({
        title: i18n.global.t('subscription.moveOffsets.failure', {
          subscriptionName,
        }),
        text: (e as Error).message,
        type: 'error',
      });
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
