import { computed, ref } from 'vue';
import axios from 'axios';

export function useInconsistentTopics() {
  const topicNames = ref<string[]>();
  const error = ref<boolean>(false);
  const loading = computed(() => !error.value && !topicNames.value);

  const topics = computed((): string[] | undefined => {
    return topicNames.value?.sort((a, b) => a.localeCompare(b));
  });

  function fetchTopicNames() {
    axios
      .get<string[]>('/consistency/inconsistencies/topics')
      .then((response) => (topicNames.value = response.data))
      .catch(() => (error.value = true));
  }

  fetchTopicNames();

  return {
    topics,
    loading,
    error,
  };
}
