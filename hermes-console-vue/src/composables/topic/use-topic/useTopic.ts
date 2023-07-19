import {
  fetchTopic as getTopic,
  fetchTopicMessagesPreview as getTopicMessagesPreview,
  fetchTopicMetrics as getTopicMetrics,
  fetchTopicOwner as getTopicOwner,
  fetchTopicSubscriptionDetails as getTopicSubscriptionDetails,
  fetchTopicSubscriptions as getTopicSubscriptions,
} from '@/api/hermes-client';
import { ref } from 'vue';
import type {
  MessagePreview,
  TopicMetrics,
  TopicWithSchema,
} from '@/api/topic';
import type { Owner } from '@/api/owner';
import type { Ref } from 'vue';
import type { Subscription } from '@/api/subscription';

export interface UseTopic {
  topic: Ref<TopicWithSchema | undefined>;
  owner: Ref<Owner | undefined>;
  messages: Ref<MessagePreview[] | undefined>;
  metrics: Ref<TopicMetrics | undefined>;
  subscriptions: Ref<Subscription[] | undefined>;
  loading: Ref<boolean>;
  error: Ref<UseTopicErrors>;
  fetchTopic: () => Promise<void>;
}

export interface UseTopicErrors {
  fetchTopic: Error | null;
  fetchOwner: Error | null;
  fetchTopicMessagesPreview: Error | null;
  fetchTopicMetrics: Error | null;
  fetchSubscriptions: Error | null;
}

export function useTopic(topicName: string): UseTopic {
  const topic = ref<TopicWithSchema>();
  const owner = ref<Owner>();
  const messages = ref<MessagePreview[]>();
  const metrics = ref<TopicMetrics>();
  const subscriptions = ref<Subscription[]>();
  const loading = ref(false);
  const error = ref<UseTopicErrors>({
    fetchTopic: null,
    fetchOwner: null,
    fetchTopicMessagesPreview: null,
    fetchTopicMetrics: null,
    fetchSubscriptions: null,
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

  return {
    topic,
    owner,
    messages,
    metrics,
    subscriptions,
    loading,
    error,
    fetchTopic,
  };
}
