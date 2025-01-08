import {
  removeTopic as deleteTopic,
  fetchTopic,
  fetchOfflineClientsSource as getOfflineClientsSource,
  fetchTopic as getTopic,
  fetchTopicClients as getTopicClients,
  fetchTopicMessagesPreview as getTopicMessagesPreview,
  fetchTopicMetrics as getTopicMetrics,
  fetchOwner as getTopicOwner,
  fetchTopicSubscriptionDetails as getTopicSubscriptionDetails,
  fetchTopicSubscriptions as getTopicSubscriptions,
  getTopicTrackingUrls,
} from '@/api/hermes-client';
import { dispatchErrorNotification } from '@/utils/notification-utils';
import { ref } from 'vue';
import { useGlobalI18n } from '@/i18n';
import { useNotificationsStore } from '@/store/app-notifications/useAppNotifications';
import type { ContentType } from '@/api/content-type';
import type {
  MessagePreview,
  TopicMetrics,
  TopicWithSchema,
} from '@/api/topic';
import type { OfflineClientsSource } from '@/api/offline-clients-source';
import type { Owner } from '@/api/owner';
import type { Ref } from 'vue';
import type { Subscription } from '@/api/subscription';
import type { TrackingUrl } from '@/api/tracking-url';

export interface UseTopic {
  topic: Ref<TopicWithSchema | undefined>;
  owner: Ref<Owner | undefined>;
  messages: Ref<MessagePreview[] | undefined>;
  metrics: Ref<TopicMetrics | undefined>;
  subscriptions: Ref<Subscription[] | undefined>;
  offlineClientsSource: Ref<OfflineClientsSource | undefined>;
  trackingUrls: Ref<TrackingUrl[] | undefined>;
  loading: Ref<boolean>;
  error: Ref<UseTopicErrors>;
  fetchOfflineClientsSource: () => Promise<void>;
  removeTopic: () => Promise<boolean>;
  fetchTopicClients: () => Promise<string[] | null>;
}

export interface UseTopicErrors {
  fetchTopic: Error | null;
  fetchOwner: Error | null;
  fetchTopicMessagesPreview: Error | null;
  fetchTopicMetrics: Error | null;
  fetchSubscriptions: Error | null;
  fetchOfflineClientsSource: Error | null;
  getTopicTrackingUrls: Error | null;
}

export function useTopic(topicName: string): UseTopic {
  const notificationStore = useNotificationsStore();

  const topic = ref<TopicWithSchema>();
  const owner = ref<Owner>();
  const messages = ref<MessagePreview[]>();
  const metrics = ref<TopicMetrics>();
  const subscriptions = ref<Subscription[]>();
  const offlineClientsSource = ref<OfflineClientsSource>();
  const trackingUrls = ref<TrackingUrl[]>();
  const loading = ref(false);
  const error = ref<UseTopicErrors>({
    fetchTopic: null,
    fetchOwner: null,
    fetchTopicMessagesPreview: null,
    fetchTopicMetrics: null,
    fetchSubscriptions: null,
    fetchOfflineClientsSource: null,
    getTopicTrackingUrls: null,
  });

  const fetchTopic = async () => {
    try {
      loading.value = true;
      await fetchTopicInfo();
      if (topic.value) {
        await Promise.allSettled([
          fetchTopicOwner(topic.value.owner.id, topic.value.owner.source),
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

  const fetchTopicOwner = async (ownerId: string, ownerSource: string) => {
    try {
      owner.value = (await getTopicOwner(ownerId, ownerSource)).data;
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

  const fetchTopicTrackingUrls = async () => {
    try {
      trackingUrls.value = (await getTopicTrackingUrls(topicName)).data;
    } catch (e) {
      error.value.getTopicTrackingUrls = e as Error;
    }
  };

  const removeTopic = async (): Promise<boolean> => {
    try {
      await deleteTopic(topicName);
      await notificationStore.dispatchNotification({
        text: useGlobalI18n().t('notifications.topic.delete.success', {
          topicName,
        }),
        type: 'success',
      });
      return true;
    } catch (e: any) {
      await dispatchErrorNotification(
        e,
        notificationStore,
        useGlobalI18n().t('notifications.topic.delete.failure', {
          topicName,
        }),
      );
      return false;
    }
  };

  const fetchTopicClients = async () => {
    try {
      return (await getTopicClients(topicName)).data;
    } catch (e: any) {
      dispatchErrorNotification(
        e,
        notificationStore,
        useGlobalI18n().t('notifications.topic.clients.fetch.failure'),
      );
      return null;
    }
  };

  fetchTopic();
  fetchTopicTrackingUrls();

  return {
    topic,
    owner,
    messages,
    metrics,
    subscriptions,
    offlineClientsSource,
    trackingUrls,
    loading,
    error,
    fetchOfflineClientsSource,
    removeTopic,
    fetchTopicClients,
  };
}

export interface FetchTopicContentType {
  contentType: ContentType | undefined;
  error: Error | null;
}

export async function fetchContentType(
  topicName: string,
): Promise<FetchTopicContentType> {
  try {
    const topicContentType = (await fetchTopic(topicName)).data.contentType;
    return {
      contentType: topicContentType,
      error: null,
    };
  } catch (e: any) {
    return {
      contentType: undefined,
      error: e as Error,
    };
  }
}
