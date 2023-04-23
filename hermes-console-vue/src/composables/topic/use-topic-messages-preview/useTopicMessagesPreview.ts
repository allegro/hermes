import { computed, ref } from 'vue';
import type { MessagePreview } from '@/api/topic';
import { fetchTopicMessagesPreview } from '@/api/hermes-client';

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
