import { computed, ref } from 'vue';
import type { Owner } from '@/api/owner';
import type { TopicWithSchema } from '@/api/topic';
import { fetchTopic, fetchTopicOwner } from '@/api/hermes-client';

export function useTopic(topicName: string) {
  const topic = ref<TopicWithSchema>();
  const topicError = ref(false);
  const topicIsLoading = computed(() => !topic.value && !topicError.value);
  const owner = ref<Owner>();
  const ownerError = ref(false);
  const ownerIsLoading = computed(() => !owner.value && !ownerError.value);

  fetchTopic(topicName)
    .then((response) => (topic.value = response.data))
    .catch(() => {
      topicError.value = true;
      ownerError.value = true;
      return Promise.reject();
    })
    .then((topic) => fetchTopicOwner(topic.owner.id))
    .then((response) => (owner.value = response.data))
    .catch(() => (ownerError.value = true));

  return {
    topic,
    owner,
    topicError,
    ownerError,
    topicIsLoading,
    ownerIsLoading,
  };
}
