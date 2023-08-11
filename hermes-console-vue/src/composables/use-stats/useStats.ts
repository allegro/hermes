import { fetchStats } from '@/api/hermes-client';
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
  error: Ref<boolean>;
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

  const error = ref<boolean>(false);

  const getStats = async () => {
    try {
      loading.value = true;
      const stats = (await fetchStats()).data;
      const topicStats = stats.topicStats;
      const subscriptionStats = stats.subscriptionStats;

      topicCount.value = topicStats.topicCount;
      ackAllTopicCount.value = topicStats.ackAllTopicCount;
      ackAllTopicShare.value =
        (topicStats.ackAllTopicCount / topicStats.topicCount) * 100;
      trackingEnabledTopicCount.value = topicStats.trackingEnabledTopicCount;
      trackingEnabledTopicShare.value =
        (topicStats.trackingEnabledTopicCount / topicStats.topicCount) * 100;
      avroTopicCount.value = topicStats.avroTopicCount;
      avroTopicShare.value =
        (topicStats.avroTopicCount / topicStats.topicCount) * 100;
      subscriptionCount.value = subscriptionStats.subscriptionCount;
      trackingEnabledSubscriptionCount.value =
        subscriptionStats.trackingEnabledSubscriptionCount;
      trackingEnabledSubscriptionShare.value =
        (subscriptionStats.trackingEnabledSubscriptionCount /
          subscriptionStats.subscriptionCount) *
        100;
      avroSubscriptionCount.value = subscriptionStats.avroSubscriptionCount;
      avroSubscriptionShare.value =
        (subscriptionStats.avroSubscriptionCount /
          subscriptionStats.subscriptionCount) *
        100;
    } catch (e) {
      error.value = true;
    } finally {
      loading.value = false;
    }
  };

  getStats();
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
