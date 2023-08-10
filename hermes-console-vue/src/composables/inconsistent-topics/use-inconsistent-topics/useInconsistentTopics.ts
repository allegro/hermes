import { computed, ref } from 'vue';
import type { Ref } from 'vue';
import { fetchInconsistentTopics as getInconsistentTopics } from "@/api/hermes-client";

export interface UseInconsistentTopics {
  topics: Ref<string[] | undefined>;
  loading: Ref<boolean>;
  error: Ref<UseInconsistentTopicsErrors>;
}

export interface UseInconsistentTopicsErrors {
  fetchInconsistentTopics: Error | null,
}

export function useInconsistentTopics(): UseInconsistentTopics {
  const topicNames = ref<string[]>();
  const error = ref<UseInconsistentTopicsErrors>({
    fetchInconsistentTopics: null,
  });
  const loading = ref(false);

  const topics = computed((): string[] | undefined => {
    return topicNames.value?.sort((a, b) => a.localeCompare(b));
  });

  const fetchInconsistentTopics = async () => {
    try {
      loading.value = true;
      topicNames.value = (await getInconsistentTopics()).data;
    } catch (e) {
      error.value.fetchInconsistentTopics = e as Error;
    } finally {
      loading.value = false;
    }
  }

  fetchInconsistentTopics();

  return {
    topics,
    loading,
    error,
  };
}
