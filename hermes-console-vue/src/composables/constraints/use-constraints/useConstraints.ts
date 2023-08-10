import { computed, ref } from 'vue';
import { fetchConstraints as getConstraints } from '@/api/hermes-client';
import type { Constraint, ConstraintsConfig } from '@/api/constraints';
import type { Ref } from 'vue';

export interface UseConstraints {
  topicConstraints: Ref<Record<string, Constraint> | undefined>;
  subscriptionConstraints: Ref<Record<string, Constraint> | undefined>;
  loading: Ref<boolean>;
  error: Ref<UseConstraintsErrors>;
}

export interface UseConstraintsErrors {
  fetchConstraints: Error | null;
}

export function useConstraints(): UseConstraints {
  const constraints = ref<ConstraintsConfig>();
  const error = ref<UseConstraintsErrors>({
    fetchConstraints: null,
  });
  const loading = ref(false);

  const topicConstraints = computed((): Record<string, Constraint> => {
    return constraints.value?.topicConstraints ?? {};
  });

  const subscriptionConstraints = computed((): Record<string, Constraint> => {
    return constraints.value?.subscriptionConstraints ?? {};
  });

  const fetchConstraints = async () => {
    try {
      loading.value = true;
      constraints.value = (await getConstraints()).data;
    } catch (e) {
      error.value.fetchConstraints = e as Error;
    } finally {
      loading.value = false;
    }
  };

  fetchConstraints();

  return {
    topicConstraints,
    subscriptionConstraints,
    loading,
    error,
  };
}
