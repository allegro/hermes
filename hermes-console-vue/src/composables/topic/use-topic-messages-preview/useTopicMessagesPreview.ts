import { computed, ref } from 'vue';
import axios from 'axios';
import type { MessagePreview } from '@/api/topic';
import type { ResponsePromise } from '@/utils/axios-utils';

export function useTopicMessagesPreview(topicName: string) {
  const messages = ref<MessagePreview[]>();
  const error = ref(false);
  const isLoading = computed(() => !messages.value && !error.value);

  fetchTopicMessagesPreview(topicName)
    .then((response) => (messages.value = response.data))
    .catch((err) => (error.value = err));

  return {
    messages,
    error,
    isLoading,
  };
}

const fetchTopicMessagesPreview = (
  topicName: string,
): ResponsePromise<Array<MessagePreview>> =>
  axios.get(`/topics/${topicName}/preview`);
