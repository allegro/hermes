import { ref } from 'vue';
import { sendRequest } from '@/utils/send-request';
import type { MessagePreview } from '@/api/topic';

export function useTopicMessagesPreview(topicName: string) {
  const messages = ref<Array<MessagePreview> | null>(null);
  const error = ref<any | null>(null);

  fetchTopicMessagesPreview(topicName)
    .then((data) => (messages.value = data))
    .catch((err) => (error.value = err));

  return {
    messages,
    error,
  };
}

const fetchTopicMessagesPreview = (
  topicName: string,
): Promise<Array<MessagePreview>> =>
  sendRequest(`http://localhost:3000/topics/${topicName}/preview`);
