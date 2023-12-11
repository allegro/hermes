import type { Stats } from '@/api/stats';

export const statsResponse: Stats = {
  topicStats: {
    topicCount: 100,
    trackingEnabledTopicCount: 50,
    avroTopicCount: 20,
    ackAllTopicCount: 10,
  },
  subscriptionStats: {
    subscriptionCount: 1000,
    avroSubscriptionCount: 500,
    trackingEnabledSubscriptionCount: 100,
  },
};
