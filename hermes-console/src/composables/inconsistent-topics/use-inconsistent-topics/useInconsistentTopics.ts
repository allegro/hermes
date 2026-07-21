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
  fetchInconsistentTopics: () => Promise<void>;
  removeTopicsLocally: (topics: string[]) => void;
  removeInconsistentTopic: (
    topic: string,
    notify?: boolean,
  ) => Promise<boolean>;
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
      error.value.fetchInconsistentTopics = null;
      topicNames.value = (await getInconsistentTopics()).data;
    } catch (e) {
      error.value.fetchInconsistentTopics = e as Error;
    } finally {
      loading.value = false;
    }
  };

  const removeTopicsLocally = (topics: string[]) => {
    topicNames.value = topicNames.value?.filter(
      (topic) => !topics.includes(topic),
    );
  };

  const removeInconsistentTopic = async (
    topic: string,
    notify = true,
  ): Promise<boolean> => {
    try {
      await deleteInconsistentTopic(topic);
      if (notify) {
        await notificationStore.dispatchNotification({
          text: useGlobalI18n().t(
            'notifications.inconsistentTopic.delete.success',
            {
              topic,
            },
          ),
          type: 'success',
        });
      }
      return true;
    } catch (e: any) {
      if (notify) {
        await dispatchErrorNotification(
          e,
          notificationStore,
          useGlobalI18n().t('notifications.inconsistentTopic.delete.failure', {
            topic,
          }),
        );
      }
      return false;
    }
  };

  fetchInconsistentTopics();

  return {
    topics,
    loading,
    error,
    fetchInconsistentTopics,
    removeTopicsLocally,
    removeInconsistentTopic,
  };
}
