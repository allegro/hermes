import { querySubscriptions, queryTopics } from '@/api/hermes-client';
import { ref } from 'vue';
import type { Ref } from 'vue';

export interface UseStats {
  topicCount: Ref<number | undefined>;
  ackAllTopicCount: Ref<number | undefined>;
  ackAllTopicShare: Ref<number | undefined>;
  trackingEnabledTopicCount: Ref<number | undefined>;
  trackingEnabledTopicShare: Ref<number | undefined>;
  avroTopicCount: Ref<number | undefined>;
  avroTopicShare: Ref<number | undefined>;
  subscriptionCount: Ref<number | undefined>;
  trackingEnabledSubscriptionCount: Ref<number | undefined>;
  trackingEnabledSubscriptionShare: Ref<number | undefined>;
  avroSubscriptionCount: Ref<number | undefined>;
  avroSubscriptionShare: Ref<number | undefined>;
  loading: Ref<boolean>;
  error: Ref<UseStatsErrors>;
}

export interface UseStatsErrors {
  fetchTopics: Error | null;
  fetchAckAllTopics: Error | null;
  fetchTrackingEnabledTopics: Error | null;
  fetchAvroTopics: Error | null;
  fetchSubscriptions: Error | null;
  fetchTrackingEnabledSubscriptions: Error | null;
  fetchAvroSubscriptions: Error | null;
}

export function useStats(): UseStats {
  const loading = ref(false);
  const topicCount = ref<number>();
  const ackAllTopicCount = ref<number>();
  const ackAllTopicShare = ref<number>();
  const trackingEnabledTopicCount = ref<number>();
  const trackingEnabledTopicShare = ref<number>();
  const avroTopicCount = ref<number>();
  const avroTopicShare = ref<number>();
  const subscriptionCount = ref<number>();
  const trackingEnabledSubscriptionCount = ref<number>();
  const trackingEnabledSubscriptionShare = ref<number>();
  const avroSubscriptionCount = ref<number>();
  const avroSubscriptionShare = ref<number>();

  const error = ref<UseStatsErrors>({
    fetchTopics: null,
    fetchAckAllTopics: null,
    fetchTrackingEnabledTopics: null,
    fetchAvroTopics: null,
    fetchSubscriptions: null,
    fetchTrackingEnabledSubscriptions: null,
    fetchAvroSubscriptions: null,
  });

  const fetchAllStats = async () => {
    try {
      loading.value = true;
      await Promise.allSettled([fetchTopicStats(), fetchSubscriptionStats()]);
    } finally {
      loading.value = false;
    }
  };

  const fetchTopicStats = async () => {
    await fetchTopics();
    const count = topicCount.value;
    if (count) {
      await Promise.allSettled([
        fetchAckAllTopics(count),
        fetchTrackingEnabledTopics(count),
        fetchAvroTopics(count),
      ]);
    }
  };

  const fetchSubscriptionStats = async () => {
    await fetchSubscriptions();
    const count = subscriptionCount.value;
    if (count) {
      await Promise.allSettled([
        fetchTrackingEnabledSubscriptions(count),
        fetchAvroSubscriptions(count),
      ]);
    }
  };

  const fetchTopics = async () => {
    try {
      topicCount.value = (await queryTopics({})).data.length;
    } catch (e) {
      error.value.fetchTopics = e as Error;
    }
  };

  const fetchAckAllTopics = async (topicCount: number) => {
    try {
      const count = (await queryTopics({ query: { ack: { eq: 'ALL' } } })).data
        .length;
      ackAllTopicCount.value = count;
      ackAllTopicShare.value = (count / topicCount) * 100;
    } catch (e) {
      error.value.fetchAckAllTopics = e as Error;
    }
  };

  const fetchTrackingEnabledTopics = async (topicCount: number) => {
    try {
      const count = (
        await queryTopics({ query: { trackingEnabled: { eq: true } } })
      ).data.length;
      trackingEnabledTopicCount.value = count;
      trackingEnabledTopicShare.value = (count / topicCount) * 100;
    } catch (e) {
      error.value.fetchTrackingEnabledTopics = e as Error;
    }
  };

  const fetchAvroTopics = async (topicCount: number) => {
    try {
      const count = (
        await queryTopics({ query: { contentType: { eq: 'AVRO' } } })
      ).data.length;
      avroTopicCount.value = count;
      avroTopicShare.value = (count / topicCount) * 100;
    } catch (e) {
      error.value.fetchAvroTopics = e as Error;
    }
  };

  const fetchSubscriptions = async () => {
    try {
      subscriptionCount.value = (await querySubscriptions({})).data.length;
    } catch (e) {
      error.value.fetchSubscriptions = e as Error;
    }
  };

  const fetchTrackingEnabledSubscriptions = async (
    subscriptionCount: number,
  ) => {
    try {
      const count = (
        await querySubscriptions({ query: { trackingEnabled: { eq: true } } })
      ).data.length;
      trackingEnabledSubscriptionCount.value = count;
      trackingEnabledSubscriptionShare.value =
        (count / subscriptionCount) * 100;
    } catch (e) {
      error.value.fetchTrackingEnabledSubscriptions = e as Error;
    }
  };

  const fetchAvroSubscriptions = async (subscriptionCount: number) => {
    try {
      const count = (
        await querySubscriptions({ query: { contentType: { eq: 'AVRO' } } })
      ).data.length;
      avroSubscriptionCount.value = count;
      avroSubscriptionShare.value = (count / subscriptionCount) * 100;
    } catch (e) {
      error.value.fetchAvroSubscriptions = e as Error;
    }
  };

  fetchAllStats();
  return {
    topicCount,
    ackAllTopicCount,
    ackAllTopicShare,
    trackingEnabledTopicCount,
    trackingEnabledTopicShare,
    avroTopicCount,
    avroTopicShare,
    subscriptionCount,
    trackingEnabledSubscriptionCount,
    trackingEnabledSubscriptionShare,
    avroSubscriptionCount,
    avroSubscriptionShare,
    loading,
    error,
  };
}
