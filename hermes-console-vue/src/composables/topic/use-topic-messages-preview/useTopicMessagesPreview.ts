import { fetchTopicMessagesPreview } from '@/api/hermes-client';
import { useFetchedData } from '@/composables/use-fetched-data/useFetchedData';

export const useTopicMessagesPreview = (topicName: string) =>
  useFetchedData({ request: () => fetchTopicMessagesPreview(topicName) });
