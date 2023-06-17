import { computed, ref } from 'vue';
import type { ComputedRef, Ref } from 'vue';
import type { ResponsePromise } from '@/utils/axios-utils';

type ReturnType<Response> = {
  data: Ref<Response | undefined>;
  error: Ref<boolean>;
  isLoading: ComputedRef<boolean>;
};

export function useFetchedData<Response>({
  request,
}: {
  request: () => ResponsePromise<Response>;
}): ReturnType<Response> {
  const data = ref<Response>();
  const error = ref(false);
  const isLoading = computed(() => !error.value && !data.value);

  request()
    .then((response) => (data.value = response.data))
    .catch(() => (error.value = true));

  return {
    data,
    error,
    isLoading,
  };
}
