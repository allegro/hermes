import {
  fetchSubscription as getSubscription,
  fetchSubscriptionHealth as getSubscriptionHealth,
  fetchSubscriptionLastUndeliveredMessage as getSubscriptionLastUndeliveredMessage,
  fetchSubscriptionMetrics as getSubscriptionMetrics,
  fetchSubscriptionUndeliveredMessages as getSubscriptionUndeliveredMessages,
} from '@/api/hermes-client';
import { ref } from 'vue';
import type { Ref } from 'vue';
import type { SentMessageTrace } from '@/api/subscription-undelivered';
import type { Subscription } from '@/api/subscription';
import type { SubscriptionHealth } from '@/api/subscription-health';
import type { SubscriptionMetrics } from '@/api/subscription-metrics';

export interface UseSubscription {
  subscription: Ref<Subscription | undefined>;
  subscriptionMetrics: Ref<SubscriptionMetrics | undefined>;
  subscriptionHealth: Ref<SubscriptionHealth | undefined>;
  subscriptionUndeliveredMessages: Ref<SentMessageTrace[] | null>;
  subscriptionLastUndeliveredMessage: Ref<SentMessageTrace | null>;
  loading: Ref<boolean>;
  error: Ref<UseSubscriptionsErrors>;
}

export interface UseSubscriptionsErrors {
  fetchSubscription: Error | null;
  fetchSubscriptionMetrics: Error | null;
  fetchSubscriptionHealth: Error | null;
  fetchSubscriptionUndeliveredMessages: Error | null;
  fetchSubscriptionLastUndeliveredMessage: Error | null;
}

export function useSubscription(
  topicName: string,
  subscriptionName: string,
): UseSubscription {
  const subscription = ref<Subscription>();
  const subscriptionMetrics = ref<SubscriptionMetrics>();
  const subscriptionHealth = ref<SubscriptionHealth>();
  const subscriptionUndeliveredMessages = ref<SentMessageTrace[]>([]);
  const subscriptionLastUndeliveredMessage = ref<SentMessageTrace | null>(null);
  const loading = ref(false);
  const error = ref<UseSubscriptionsErrors>({
    fetchSubscription: null,
    fetchSubscriptionMetrics: null,
    fetchSubscriptionHealth: null,
    fetchSubscriptionUndeliveredMessages: null,
    fetchSubscriptionLastUndeliveredMessage: null,
  });

  const fetchSubscription = async () => {
    try {
      loading.value = true;
      await fetchSubscriptionInfo();
      if (subscription.value) {
        await Promise.allSettled([
          fetchSubscriptionMetrics(),
          fetchSubscriptionHealth(),
          fetchSubscriptionUndeliveredMessages(),
          fetchSubscriptionLastUndeliveredMessage(),
        ]);
      }
    } finally {
      loading.value = false;
    }
  };

  const fetchSubscriptionInfo = async () => {
    try {
      subscription.value = (
        await getSubscription(topicName, subscriptionName)
      ).data;
    } catch (e) {
      error.value.fetchSubscription = e as Error;
    }
  };

  const fetchSubscriptionMetrics = async () => {
    try {
      subscriptionMetrics.value = (
        await getSubscriptionMetrics(topicName, subscriptionName)
      ).data;
    } catch (e) {
      error.value.fetchSubscriptionMetrics = e as Error;
    }
  };

  const fetchSubscriptionHealth = async () => {
    try {
      subscriptionHealth.value = (
        await getSubscriptionHealth(topicName, subscriptionName)
      ).data;
    } catch (e) {
      error.value.fetchSubscriptionHealth = e as Error;
    }
  };

  const fetchSubscriptionUndeliveredMessages = async () => {
    try {
      subscriptionUndeliveredMessages.value = (
        await getSubscriptionUndeliveredMessages(topicName, subscriptionName)
      ).data;
    } catch (e) {
      error.value.fetchSubscriptionUndeliveredMessages = e as Error;
    }
  };

  const fetchSubscriptionLastUndeliveredMessage = async () => {
    try {
      subscriptionLastUndeliveredMessage.value = (
        await getSubscriptionLastUndeliveredMessage(topicName, subscriptionName)
      ).data;
    } catch (e) {
      error.value.fetchSubscriptionLastUndeliveredMessage = e as Error;
    }
  };

  fetchSubscription();

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
