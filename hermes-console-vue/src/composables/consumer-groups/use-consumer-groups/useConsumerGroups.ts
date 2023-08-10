import { fetchConsumerGroups as getConsumerGroups } from '@/api/hermes-client';
import { ref } from 'vue';
import type { ConsumerGroup } from '@/api/consumer-group';
import type { Ref } from 'vue';

export interface UseConsumerGroups {
  consumerGroups: Ref<ConsumerGroup[] | undefined>;
  loading: Ref<boolean>;
  error: Ref<UseConsumerGroupsErrors>;
}

export interface UseConsumerGroupsErrors {
  fetchConsumerGroups: Error | null;
}

export function useConsumerGroups(
  topicName: string,
  subscriptionName: string,
): UseConsumerGroups {
  const consumerGroups = ref<ConsumerGroup[]>();
  const error = ref<UseConsumerGroupsErrors>({
    fetchConsumerGroups: null,
  });
  const loading = ref(false);

  const fetchConsumerGroups = async () => {
    try {
      loading.value = true;
      consumerGroups.value = (
        await getConsumerGroups(topicName, subscriptionName)
      ).data;
    } catch (e) {
      error.value.fetchConsumerGroups = e as Error;
    } finally {
      loading.value = false;
    }
  };

  fetchConsumerGroups();

  return {
    consumerGroups,
    loading,
    error,
  };
}
