import { computed, ref } from 'vue';
import axios from 'axios';
import type { ConsumerGroup } from '@/api/consumer-group';

export function useConsumerGroups(topicName: string, subscriptionName: string) {
  const consumerGroups = ref<ConsumerGroup[]>();
  const error = ref<boolean>(false);

  const loading = computed(() => !error.value && !consumerGroups.value);

  function fetchConsumerGroups() {
    axios
      .get<ConsumerGroup[]>(
        `/topics/${topicName}/subscriptions/${subscriptionName}/consumer-groups`,
      )
      .then((response) => (consumerGroups.value = response.data))
      .catch(() => (error.value = true));
  }

  fetchConsumerGroups();

  return {
    consumerGroups,
    loading,
    error,
  };
}
