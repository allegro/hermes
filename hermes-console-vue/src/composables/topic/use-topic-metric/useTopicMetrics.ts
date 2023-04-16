import { computed, ref } from 'vue';
import axios from 'axios';
import type { AxiosResponse } from 'axios';
import type { TopicMetrics } from '@/api/topic';

export function useTopicMetrics(topic: string) {
  const metrics = ref<TopicMetrics>();
  const error = ref(false);
  const isLoading = computed(() => !error.value && !metrics.value);

  fetchTopicMetrics(topic)
    .then((response) => (metrics.value = response.data))
    .catch(() => (error.value = true));

  return {
    metrics,
    error,
    isLoading,
  };
}

const fetchTopicMetrics = (
  topic: String,
): Promise<AxiosResponse<TopicMetrics>> =>
  axios.get<TopicMetrics>(`/topics/${topic}/metrics`);
