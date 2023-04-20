import { computed, ref } from 'vue';
import axios from 'axios';
import type { ResponsePromise } from '@/utils/axios-utils';
import type { Subscription } from '@/api/subscription';

export function useSubscriptionsList(topicName: string) {
  const subscriptions = ref<Subscription[]>();
  const error = ref(false);
  const isLoading = computed(
    () => subscriptions.value === undefined && error.value === false,
  );

  fetchTopicSubscriptions(topicName)
    .then((response) => {
      const detailedSubscriptionsPromises = response.data.map((subscription) =>
        fetchTopicSubscriptionDetails(topicName, subscription).catch(() => {
          error.value = true;
          return undefined;
        }),
      );

      Promise.allSettled(detailedSubscriptionsPromises).then((responses) => {
        subscriptions.value = responses
          .filter(
            (response) => response.status === 'fulfilled' && response.value,
          )
          .map((response) => response as { value: { data: Subscription } })
          .map((response) => response.value.data);
      });
    })
    .catch(() => (error.value = true));

  return {
    subscriptions,
    error,
    isLoading,
  };
}

const fetchTopicSubscriptions = (
  topicName: string,
): ResponsePromise<string[]> => axios.get(`/topics/${topicName}/subscriptions`);

const fetchTopicSubscriptionDetails = (
  topicName: string,
  subscription: string,
): ResponsePromise<Subscription> =>
  axios.get(`/topics/${topicName}/subscriptions/${subscription}`);
