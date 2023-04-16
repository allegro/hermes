import { computed, ref } from 'vue';
import axios from 'axios';
import type { ResponsePromise } from '@/utils/axios-utils';
import type { Subscription } from '@/api/subscription';

export function useSubscriptionsList(topicName: string) {
  const subscriptions = ref<Subscription[]>();
  const subscriptionsError = ref(false);
  const subscriptionsAreLoading = computed(
    () => !subscriptions.value && !subscriptionsError.value,
  );

  fetchTopicSubscriptions(topicName)
    .then((response) => {
      const detailedSubscriptionsPromises = response.data.map((subscription) =>
        fetchTopicSubscriptionDetails(topicName, subscription),
      );
      Promise.all(detailedSubscriptionsPromises).then((responses) => {
        subscriptions.value = responses.map((response) => response.data);
      });
    })
    .catch(() => (subscriptionsError.value = true));

  return {
    subscriptions,
    subscriptionsError,
    subscriptionsAreLoading,
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
