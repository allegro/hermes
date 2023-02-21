import { computed, ref } from 'vue';
import axios from 'axios';
import type { SentMessageTrace } from '@/api/subscription-undelivered';
import type { Subscription } from '@/api/subscription';
import type { SubscriptionHealth } from '@/api/subscription-health';
import type { SubscriptionMetrics } from '@/api/subscription-metrics';

export function useSubscription(topicName: string, subscriptionName: string) {
  const subscription = ref<Subscription>();
  const subscriptionMetrics = ref<SubscriptionMetrics>();
  const subscriptionHealth = ref<SubscriptionHealth>();
  const subscriptionUndeliveredMessages = ref<SentMessageTrace[]>();
  const subscriptionLastUndeliveredMessage = ref<SentMessageTrace | null>();

  const error = ref<boolean>(false);

  const loading = computed(
    () =>
      !error.value &&
      !(
        subscription.value &&
        subscriptionMetrics.value &&
        subscriptionHealth.value &&
        subscriptionUndeliveredMessages.value &&
        subscriptionLastUndeliveredMessage.value !== undefined
      ),
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
      .then((response) => (subscriptionMetrics.value = response.data))
      .catch(() => (error.value = true));
  }

  function fetchSubscriptionHealth() {
    axios
      .get<SubscriptionHealth>(
        `/topics/${topicName}/subscriptions/${subscriptionName}/health`,
      )
      .then((response) => (subscriptionHealth.value = response.data))
      .catch(() => (error.value = true));
  }

  function fetchSubscriptionUndeliveredMessages() {
    axios
      .get<SentMessageTrace[]>(
        `/topics/${topicName}/subscriptions/${subscriptionName}/undelivered`,
      )
      .then(
        (response) => (subscriptionUndeliveredMessages.value = response.data),
      )
      .catch(() => (subscriptionUndeliveredMessages.value = []));
  }

  function fetchSubscriptionLastUndeliveredMessage() {
    axios
      .get<SentMessageTrace>(
        `/topics/${topicName}/subscriptions/${subscriptionName}/undelivered/last`,
      )
      .then(
        (response) =>
          (subscriptionLastUndeliveredMessage.value = response.data),
      )
      .catch(() => (subscriptionLastUndeliveredMessage.value = null));
  }

  fetchSubscription();
  fetchSubscriptionMetrics();
  fetchSubscriptionHealth();
  fetchSubscriptionUndeliveredMessages();
  fetchSubscriptionLastUndeliveredMessage();

  return {
    subscription,
    subscriptionMetrics,
    subscriptionHealth,
    subscriptionUndeliveredMessages,
    subscriptionLastUndeliveredMessage,
    loading,
    error,
  };
}
