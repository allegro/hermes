import type { ResponsePromise } from '@/utils/axios-utils';
import type { ComputedRef, Ref } from 'vue';
import { computed, ref } from 'vue';

type ReturnType<RESPONSE> = {
  data: Ref<RESPONSE | undefined>;
  error: Ref<boolean>;
  isLoading: ComputedRef<boolean>;
};

export function useFetchedData<RESPONSE>({
  request,
}: {
  request: () => ResponsePromise<RESPONSE>;
}): ReturnType<RESPONSE> {
  const data = ref<RESPONSE>();
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
