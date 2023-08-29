import {
  activateSubscription as activate,
  removeSubscription as deleteSubscription,
  fetchOwner as getOwner,
  fetchSubscription as getSubscription,
  fetchSubscriptionHealth as getSubscriptionHealth,
  fetchSubscriptionLastUndeliveredMessage as getSubscriptionLastUndeliveredMessage,
  fetchSubscriptionMetrics as getSubscriptionMetrics,
  fetchSubscriptionUndeliveredMessages as getSubscriptionUndeliveredMessages,
  suspendSubscription as suspend,
} from '@/api/hermes-client';
import { ref } from 'vue';
import { useGlobalI18n } from '@/i18n';
import { useNotificationsStore } from '@/store/app-notifications/useAppNotifications';
import type { Owner } from '@/api/owner';
import type { Ref } from 'vue';
import type { SentMessageTrace } from '@/api/subscription-undelivered';
import type { Subscription } from '@/api/subscription';
import type { SubscriptionHealth } from '@/api/subscription-health';
import type { SubscriptionMetrics } from '@/api/subscription-metrics';

export interface UseSubscription {
  subscription: Ref<Subscription | undefined>;
  owner: Ref<Owner | undefined>;
  subscriptionMetrics: Ref<SubscriptionMetrics | undefined>;
  subscriptionHealth: Ref<SubscriptionHealth | undefined>;
  subscriptionUndeliveredMessages: Ref<SentMessageTrace[] | null>;
  subscriptionLastUndeliveredMessage: Ref<SentMessageTrace | null>;
  loading: Ref<boolean>;
  error: Ref<UseSubscriptionsErrors>;
  removeSubscription: () => Promise<boolean>;
  suspendSubscription: () => Promise<boolean>;
  activateSubscription: () => Promise<boolean>;
}

export interface UseSubscriptionsErrors {
  fetchSubscription: Error | null;
  fetchOwner: Error | null;
  fetchSubscriptionMetrics: Error | null;
  fetchSubscriptionHealth: Error | null;
  fetchSubscriptionUndeliveredMessages: Error | null;
  fetchSubscriptionLastUndeliveredMessage: Error | null;
}

export function useSubscription(
  topicName: string,
  subscriptionName: string,
): UseSubscription {
  const notificationStore = useNotificationsStore();

  const subscription = ref<Subscription>();
  const owner = ref<Owner>();
  const subscriptionMetrics = ref<SubscriptionMetrics>();
  const subscriptionHealth = ref<SubscriptionHealth>();
  const subscriptionUndeliveredMessages = ref<SentMessageTrace[]>([]);
  const subscriptionLastUndeliveredMessage = ref<SentMessageTrace | null>(null);
  const loading = ref(false);
  const error = ref<UseSubscriptionsErrors>({
    fetchSubscription: null,
    fetchOwner: null,
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
          fetchSubscriptionOwner(subscription.value.owner.id),
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

  const fetchSubscriptionOwner = async (ownerId: string) => {
    try {
      owner.value = (await getOwner(ownerId)).data;
    } catch (e) {
      error.value.fetchOwner = e as Error;
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

  const removeSubscription = async (): Promise<boolean> => {
    try {
      await deleteSubscription(topicName, subscriptionName);
      notificationStore.dispatchNotification({
        text: useGlobalI18n().t('notifications.subscription.delete.success', {
          subscriptionName,
        }),
        type: 'success',
      });
      return true;
    } catch (e) {
      notificationStore.dispatchNotification({
        title: useGlobalI18n().t('notifications.subscription.delete.failure', {
          subscriptionName,
        }),
        text: (e as Error).message,
        type: 'error',
      });
      return false;
    }
  };

  const suspendSubscription = async (): Promise<boolean> => {
    try {
      await suspend(topicName, subscriptionName);
      notificationStore.dispatchNotification({
        text: useGlobalI18n().t('notifications.subscription.suspend.success', {
          subscriptionName,
        }),
        type: 'success',
      });
      return true;
    } catch (e) {
      notificationStore.dispatchNotification({
        title: useGlobalI18n().t('notifications.subscription.suspend.failure', {
          subscriptionName,
        }),
        text: (e as Error).message,
        type: 'error',
      });
      return false;
    }
  };

  const activateSubscription = async (): Promise<boolean> => {
    try {
      await activate(topicName, subscriptionName);
      notificationStore.dispatchNotification({
        text: useGlobalI18n().t('notifications.subscription.activate.success', {
          subscriptionName,
        }),
        type: 'success',
      });
      return true;
    } catch (e) {
      notificationStore.dispatchNotification({
        title: useGlobalI18n().t(
          'notifications.subscription.activate.failure',
          {
            subscriptionName,
          },
        ),
        text: (e as Error).message,
        type: 'error',
      });
      return false;
    }
  };

  fetchSubscription();

  return {
    subscription,
    owner,
    subscriptionMetrics,
    subscriptionHealth,
    subscriptionUndeliveredMessages,
    subscriptionLastUndeliveredMessage,
    loading,
    error,
    removeSubscription,
    suspendSubscription,
    activateSubscription,
  };
}
