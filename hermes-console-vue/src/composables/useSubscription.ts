import { computed, ref } from 'vue';
import axios from 'axios';
import type { Subscription } from '@/api/subscription';
import type { SubscriptionHealth } from '@/api/subscription-health';
import type { SubscriptionMetrics } from '@/api/subscription-metrics';

export function useSubscription(topicName: string, subscriptionName: string) {
  const subscription = ref<Subscription>();
  const metrics = ref<SubscriptionMetrics>();
  const health = ref<SubscriptionHealth>();

  const error = ref<boolean>(false);

  const loading = computed(
    () => (!subscription.value || !metrics.value) && !error.value,
  );

  function fetchSubscription() {
    axios
      .get<Subscription>(
        `/topics/${topicName}/subscriptions/${subscriptionName}`,
      )
      .then((response) => (subscription.value = response.data))
      .catch(() => (error.value = true));
  }

  function fetchSubscriptionMetrics() {
    axios
      .get<SubscriptionMetrics>(
        `/topics/${topicName}/subscriptions/${subscriptionName}/metrics`,
      )
      .then((response) => (metrics.value = response.data))
      .catch(() => (error.value = true));
  }

  function fetchSubscriptionHealth() {
    axios
      .get<SubscriptionHealth>(
        `/topics/${topicName}/subscriptions/${subscriptionName}/health`,
      )
      .then((response) => (health.value = response.data))
      .catch(() => (error.value = true));
  }

  fetchSubscription();
  fetchSubscriptionMetrics();
  fetchSubscriptionHealth();

  return {
    subscription,
    metrics,
    health,
    loading,
    error,
  };
}
