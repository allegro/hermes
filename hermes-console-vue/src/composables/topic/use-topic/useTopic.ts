import {
  removeTopic as deleteTopic,
  fetchOfflineClientsSource as getOfflineClientsSource,
  fetchTopic as getTopic,
  fetchTopicMessagesPreview as getTopicMessagesPreview,
  fetchTopicMetrics as getTopicMetrics,
  fetchOwner as getTopicOwner,
  fetchTopicSubscriptionDetails as getTopicSubscriptionDetails,
  fetchTopicSubscriptions as getTopicSubscriptions,
} from '@/api/hermes-client';
import { ref } from 'vue';
import { useGlobalI18n } from '@/i18n';
import { useNotificationsStore } from '@/store/app-notifications/useAppNotifications';
import type {
  MessagePreview,
  TopicMetrics,
  TopicWithSchema,
} from '@/api/topic';
import type { OfflineClientsSource } from '@/api/offline-clients-source';
import type { Owner } from '@/api/owner';
import type { Ref } from 'vue';
import type { Subscription } from '@/api/subscription';

export interface UseTopic {
  topic: Ref<TopicWithSchema | undefined>;
  owner: Ref<Owner | undefined>;
  messages: Ref<MessagePreview[] | undefined>;
  metrics: Ref<TopicMetrics | undefined>;
  subscriptions: Ref<Subscription[] | undefined>;
  offlineClientsSource: Ref<OfflineClientsSource | undefined>;
  loading: Ref<boolean>;
  error: Ref<UseTopicErrors>;
  fetchOfflineClientsSource: () => Promise<void>;
  removeTopic: () => Promise<boolean>;
}

export interface UseTopicErrors {
  fetchTopic: Error | null;
  fetchOwner: Error | null;
  fetchTopicMessagesPreview: Error | null;
  fetchTopicMetrics: Error | null;
  fetchSubscriptions: Error | null;
  fetchOfflineClientsSource: Error | null;
}

export function useTopic(topicName: string): UseTopic {
  const notificationStore = useNotificationsStore();

  const topic = ref<TopicWithSchema>();
  const owner = ref<Owner>();
  const messages = ref<MessagePreview[]>();
  const metrics = ref<TopicMetrics>();
  const subscriptions = ref<Subscription[]>();
  const offlineClientsSource = ref<OfflineClientsSource>();
  const loading = ref(false);
  const error = ref<UseTopicErrors>({
    fetchTopic: null,
    fetchOwner: null,
    fetchTopicMessagesPreview: null,
    fetchTopicMetrics: null,
    fetchSubscriptions: null,
    fetchOfflineClientsSource: null,
  });

  const fetchTopic = async () => {
    try {
      loading.value = true;
      await fetchTopicInfo();
      if (topic.value) {
        await Promise.allSettled([
          fetchTopicOwner(topic.value.owner.id),
          fetchTopicMessagesPreview(),
          fetchTopicMetrics(),
          fetchSubscriptions(),
        ]);
      }
    } finally {
      loading.value = false;
    }
  };

  const fetchTopicInfo = async () => {
    try {
      topic.value = (await getTopic(topicName)).data;
    } catch (e) {
      error.value.fetchTopic = e as Error;
    }
  };

  const fetchTopicOwner = async (ownerId: string) => {
    try {
      owner.value = (await getTopicOwner(ownerId)).data;
    } catch (e) {
      error.value.fetchOwner = e as Error;
    }
  };

  const fetchTopicMessagesPreview = async () => {
    try {
      messages.value = (await getTopicMessagesPreview(topicName)).data;
    } catch (e) {
      error.value.fetchTopicMessagesPreview = e as Error;
    }
  };

  const fetchTopicMetrics = async () => {
    try {
      metrics.value = (await getTopicMetrics(topicName)).data;
    } catch (e) {
      error.value.fetchTopicMetrics = e as Error;
    }
  };

  const fetchSubscriptions = async () => {
    try {
      const subscriptionsList = (await getTopicSubscriptions(topicName)).data;
      const subscriptionsDetails = subscriptionsList.map(async (subscription) =>
        getTopicSubscriptionDetails(topicName, subscription),
      );
      const results = await Promise.allSettled(subscriptionsDetails);
      subscriptions.value = results
        .filter((result) => result.status === 'fulfilled' && result.value)
        .map(
          (subscription) => subscription as { value: { data: Subscription } },
        )
        .map((subscription) => subscription.value.data);
      const rejectedResult = results.find(
        (result) => result.status === 'rejected',
      );
      if (rejectedResult) {
        error.value.fetchSubscriptions = new Error(
          (rejectedResult as PromiseRejectedResult).reason,
        );
      }
    } catch (e) {
      error.value.fetchSubscriptions = e as Error;
    }
  };

  const fetchOfflineClientsSource = async () => {
    try {
      offlineClientsSource.value = (
        await getOfflineClientsSource(topicName)
      ).data;
    } catch (e) {
      error.value.fetchOfflineClientsSource = e as Error;
    }
  };

  const removeTopic = async (): Promise<boolean> => {
    try {
      await deleteTopic(topicName);
      notificationStore.dispatchNotification({
        text: useGlobalI18n().t('notifications.topic.delete.success', {
          topicName,
        }),
        type: 'success',
      });
      return true;
    } catch (e: any) {
      const text = e.response?.data?.message
        ? e.response.data.message
        : 'Unknown error occurred';
      notificationStore.dispatchNotification({
        title: useGlobalI18n().t('notifications.topic.delete.failure', {
          topicName,
        }),
        text,
        type: 'error',
      });
      return false;
    }
  };

  fetchTopic();

  return {
    topic,
    owner,
    messages,
    metrics,
    subscriptions,
    offlineClientsSource,
    loading,
    error,
    fetchOfflineClientsSource,
    removeTopic,
  };
}
