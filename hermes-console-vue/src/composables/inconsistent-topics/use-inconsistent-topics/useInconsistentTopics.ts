import { computed, ref } from 'vue';
import {
  removeInconsistentTopic as deleteInconsistentTopic,
  fetchInconsistentTopics as getInconsistentTopics,
} from '@/api/hermes-client';
import { dispatchErrorNotification } from '@/utils/notification-utils';
import { useGlobalI18n } from '@/i18n';
import { useNotificationsStore } from '@/store/app-notifications/useAppNotifications';
import type { Ref } from 'vue';

export interface UseInconsistentTopics {
  topics: Ref<string[] | undefined>;
  loading: Ref<boolean>;
  error: Ref<UseInconsistentTopicsErrors>;
  removeInconsistentTopic: (topic: string) => Promise<boolean>;
}

export interface UseInconsistentTopicsErrors {
  fetchInconsistentTopics: Error | null;
}

export function useInconsistentTopics(): UseInconsistentTopics {
  const notificationStore = useNotificationsStore();

  const topicNames = ref<string[]>();
  const error = ref<UseInconsistentTopicsErrors>({
    fetchInconsistentTopics: null,
  });
  const loading = ref(false);

  const topics = computed((): string[] | undefined => {
    return topicNames.value?.sort((a, b) => a.localeCompare(b));
  });

  const fetchInconsistentTopics = async () => {
    try {
      loading.value = true;
      topicNames.value = (await getInconsistentTopics()).data;
    } catch (e) {
      error.value.fetchInconsistentTopics = e as Error;
    } finally {
      loading.value = false;
    }
  };

  const removeInconsistentTopic = async (topic: string): Promise<boolean> => {
    try {
      await deleteInconsistentTopic(topic);
      notificationStore.dispatchNotification({
        text: useGlobalI18n().t(
          'notifications.inconsistentTopic.delete.success',
          {
            topic,
          },
        ),
        type: 'success',
      });
      return true;
    } catch (e: any) {
      dispatchErrorNotification(
        e,
        notificationStore,
        useGlobalI18n().t('notifications.inconsistentTopic.delete.failure', {
          topic,
        }),
      );
      return false;
    }
  };

  fetchInconsistentTopics();

  return {
    topics,
    loading,
    error,
    removeInconsistentTopic,
  };
}
