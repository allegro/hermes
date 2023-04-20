import { computed, ref } from 'vue';
import axios from 'axios';
import type { Constraint, ConstraintsConfig } from '@/api/constraints';

export function useConstraints() {
  const constraints = ref<ConstraintsConfig>();
  const error = ref<boolean>(false);

  const loading = computed(() => !error.value && !constraints.value);

  const topicConstraints = computed((): Record<string, Constraint> => {
    return constraints.value?.topicConstraints ?? {};
  });

  const subscriptionConstraints = computed((): Record<string, Constraint> => {
    return constraints.value?.subscriptionConstraints ?? {};
  });

  function fetchConstraints() {
    axios
      .get<ConstraintsConfig>('/workload-constraints')
      .then((response) => (constraints.value = response.data))
      .catch(() => (error.value = true));
  }

  fetchConstraints();

  return {
    topicConstraints,
    subscriptionConstraints,
    loading,
    error,
  };
}
