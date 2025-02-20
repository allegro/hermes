import { fetchInactiveTopics as getInactiveTopics } from '@/api/hermes-client';
import { ref } from 'vue';
import type { InactiveTopic } from '@/api/inactive-topics';
import type { Ref } from 'vue';

export interface UseInactiveTopics {
  inactiveTopics: Ref<InactiveTopic[] | undefined>;
  loading: Ref<boolean>;
  error: Ref<UseInactiveTopicsErrors>;
}

export interface UseInactiveTopicsErrors {
  fetchInactiveTopics: Error | null;
}

export function useInactiveTopics(): UseInactiveTopics {
  const inactiveTopics = ref<InactiveTopic[]>();
  const error = ref<UseInactiveTopicsErrors>({
    fetchInactiveTopics: null,
  });
  const loading = ref(false);

  const fetchInactiveTopics = async () => {
    try {
      loading.value = true;
      inactiveTopics.value = (await getInactiveTopics()).data;
    } catch (e) {
      error.value.fetchInactiveTopics = e as Error;
    } finally {
      loading.value = false;
    }
  };

  fetchInactiveTopics();

  return {
    inactiveTopics: inactiveTopics,
    loading,
    error,
  };
}
