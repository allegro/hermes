import { ref } from 'vue';
import axios from 'axios';
import type { Subscription } from '@/api/subscription';

export function useSubscription(topicName: string, subscriptionName: string) {
  const subscription = ref<Subscription>();
  const loading = ref<boolean>(true);
  const error = ref<boolean>(false);

  function fetchSubscription() {
    axios
      .get<Subscription>(
        `/topics/${topicName}/subscriptions/${subscriptionName}`,
      )
      .then((response) => {
        subscription.value = response.data;
        loading.value = false;
      })
      .catch(() => {
        error.value = true;
        loading.value = false;
      });
  }

  fetchSubscription();

  return {
    subscription,
    loading,
    error,
  };
}
