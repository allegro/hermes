import { useFetch } from '@/composables/useFetch';
import type { TopicMetrics } from '@/api/topic';

export function useTopicMetrics(topic: string) {
  const { data: metrics, error } = useFetch<TopicMetrics>(
    `http://localhost:3000/topics/${topic}/metrics`,
  );

  return {
    metrics,
    error,
  };
}
