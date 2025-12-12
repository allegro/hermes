import { ref } from 'vue';
import { search } from '@/api/hermes-client';
import type { Ref } from 'vue';
import type { SearchResults } from '@/api/SearchResults';

export interface UseSearch {
  results: Ref<SearchResults | null>;
  runSearch: (query: string) => void;
  loading: Ref<boolean>;
  error: Ref<UseSearchErrors>;
}

export interface UseSearchErrors {
  fetchError: Error | null;
}

export function useSearch(): UseSearch {
  const results = ref<SearchResults | null>(null);
  const error = ref<UseSearchErrors>({
    fetchError: null,
  });
  const loading = ref(false);

  const runSearch = async (query: string) => {
    try {
      loading.value = true;
      results.value = (await search(query)).data;
    } catch (e) {
      error.value.fetchError = e as Error;
    } finally {
      loading.value = false;
    }
  };

  return {
    results,
    runSearch,
    loading,
    error,
  };
}
