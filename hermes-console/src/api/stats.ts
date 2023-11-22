export interface Stats {
  topicStats: TopicStats;
  subscriptionStats: SubscriptionStats;
}

export interface TopicStats {
  topicCount: number;
  ackAllTopicCount: number;
  trackingEnabledTopicCount: number;
  avroTopicCount: number;
}

export interface SubscriptionStats {
  subscriptionCount: number;
  trackingEnabledSubscriptionCount: number;
  avroSubscriptionCount: number;
}
