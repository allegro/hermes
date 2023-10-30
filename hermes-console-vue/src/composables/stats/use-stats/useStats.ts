import { fetchStats } from '@/api/hermes-client';
import { ref } from 'vue';
import type { Ref } from 'vue';

export interface Stats {
  topicCount: number;
  ackAllTopicCount: number;
  ackAllTopicShare: number;
  trackingEnabledTopicCount: number;
  trackingEnabledTopicShare: number;
  avroTopicCount: number;
  avroTopicShare: number;
  subscriptionCount: number;
  trackingEnabledSubscriptionCount: number;
  trackingEnabledSubscriptionShare: number;
  avroSubscriptionCount: number;
  avroSubscriptionShare: number;
}

export interface UseStats {
  stats: Ref<Stats | undefined>;
  loading: Ref<boolean>;
  error: Ref<UseStatsError>;
}

export interface UseStatsError {
  fetchError: Error | null;
}

export function useStats(): UseStats {
  const stats = ref<Stats>();
  const loading = ref(false);
  const error = ref<UseStatsError>({
    fetchError: null,
  });

  const getStats = async () => {
    try {
      loading.value = true;
      const statsResponse = (await fetchStats()).data;
      const topicStats = statsResponse.topicStats;
      const subscriptionStats = statsResponse.subscriptionStats;

      stats.value = {
        topicCount: topicStats.topicCount,
        ackAllTopicCount: topicStats.ackAllTopicCount,
        ackAllTopicShare:
          (topicStats.ackAllTopicCount / topicStats.topicCount) * 100,
        trackingEnabledTopicCount: topicStats.trackingEnabledTopicCount,
        trackingEnabledTopicShare:
          (topicStats.trackingEnabledTopicCount / topicStats.topicCount) * 100,
        avroTopicCount: topicStats.avroTopicCount,
        avroTopicShare:
          (topicStats.avroTopicCount / topicStats.topicCount) * 100,
        subscriptionCount: subscriptionStats.subscriptionCount,
        trackingEnabledSubscriptionCount:
          subscriptionStats.trackingEnabledSubscriptionCount,
        trackingEnabledSubscriptionShare:
          (subscriptionStats.trackingEnabledSubscriptionCount /
            subscriptionStats.subscriptionCount) *
          100,
        avroSubscriptionCount: subscriptionStats.avroSubscriptionCount,
        avroSubscriptionShare:
          (subscriptionStats.avroSubscriptionCount /
            subscriptionStats.subscriptionCount) *
          100,
      };
    } catch (e) {
      error.value.fetchError = e as Error;
    } finally {
      loading.value = false;
    }
  };

  getStats();
  return {
    stats,
    loading,
    error,
  };
}
