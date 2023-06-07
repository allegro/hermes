import { fetchTopicMetrics } from '@/api/hermes-client';
import { useFetchedData } from '@/composables/use-fetched-data/useFetchedData';

export const useTopicMetrics = (topic: string) =>
  useFetchedData({ request: () => fetchTopicMetrics(topic) });
