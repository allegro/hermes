import type {
  MessagePreview,
  TopicMetrics,
  TopicWithSchema,
} from '@/api/topic';
import type { Owner } from '@/api/owner';
import type { Ref } from 'vue';
import type { Subscription } from '@/api/subscription';
import type { AppConfiguration } from '@/api/app-configuration';

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
