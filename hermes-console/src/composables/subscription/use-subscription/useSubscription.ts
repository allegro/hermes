import {
  activateSubscription as activate,
  removeSubscription as deleteSubscription,
  fetchOwner as getOwner,
  fetchSubscription as getSubscription,
  fetchSubscriptionHealth as getSubscriptionHealth,
  fetchSubscriptionLastUndeliveredMessage as getSubscriptionLastUndeliveredMessage,
  fetchSubscriptionMetrics as getSubscriptionMetrics,
  getSubscriptionTrackingUrls,
  fetchSubscriptionUndeliveredMessages as getSubscriptionUndeliveredMessages,
  retransmitSubscriptionMessages,
  suspendSubscription as suspend,
} from '@/api/hermes-client';
import { dispatchErrorNotification } from '@/utils/notification-utils';
import { ref } from 'vue';
import { useGlobalI18n } from '@/i18n';
import { useNotificationsStore } from '@/store/app-notifications/useAppNotifications';
import type { Owner } from '@/api/owner';
import type { Ref } from 'vue';
import type { SentMessageTrace } from '@/api/subscription-undelivered';
import type { Subscription } from '@/api/subscription';
import type { SubscriptionHealth } from '@/api/subscription-health';
import type { SubscriptionMetrics } from '@/api/subscription-metrics';
import type { TrackingUrl } from '@/api/tracking-url';

export interface UseSubscription {
  subscription: Ref<Subscription | undefined>;
  owner: Ref<Owner | undefined>;
  subscriptionMetrics: Ref<SubscriptionMetrics | undefined>;
  subscriptionHealth: Ref<SubscriptionHealth | undefined>;
  subscriptionUndeliveredMessages: Ref<SentMessageTrace[] | null>;
  subscriptionLastUndeliveredMessage: Ref<SentMessageTrace | null>;
  trackingUrls: Ref<TrackingUrl[] | undefined>;
  loading: Ref<boolean>;
  retransmitting: Ref<boolean>;
  skippingAllMessages: Ref<boolean>;
  error: Ref<UseSubscriptionsErrors>;
  removeSubscription: () => Promise<boolean>;
  suspendSubscription: () => Promise<boolean>;
  activateSubscription: () => Promise<boolean>;
  retransmitMessages: (from: string) => Promise<boolean>;
  skipAllMessages: () => Promise<boolean>;
}

export interface UseSubscriptionsErrors {
  fetchSubscription: Error | null;
  fetchOwner: Error | null;
  fetchSubscriptionMetrics: Error | null;
  fetchSubscriptionHealth: Error | null;
  fetchSubscriptionUndeliveredMessages: Error | null;
  fetchSubscriptionLastUndeliveredMessage: Error | null;
  getSubscriptionTrackingUrls: Error | null;
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
  const trackingUrls = ref<TrackingUrl[]>();
  const loading = ref(false);
  const error = ref<UseSubscriptionsErrors>({
    fetchSubscription: null,
    fetchOwner: null,
    fetchSubscriptionMetrics: null,
    fetchSubscriptionHealth: null,
    fetchSubscriptionUndeliveredMessages: null,
    fetchSubscriptionLastUndeliveredMessage: null,
    getSubscriptionTrackingUrls: null,
  });
  const retransmitting = ref(false);
  const skippingAllMessages = ref(false);
  const fetchSubscription = async () => {
    try {
      loading.value = true;
      await fetchSubscriptionInfo();
      if (subscription.value) {
        await Promise.allSettled([
          fetchSubscriptionOwner(
            subscription.value.owner.id,
            subscription.value.owner.source,
          ),
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

  const fetchSubscriptionOwner = async (
    ownerId: string,
    ownerSource: string,
  ) => {
    try {
      owner.value = (await getOwner(ownerId, ownerSource)).data;
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

  const fetchSubscriptionTrackingUrls = async () => {
    try {
      trackingUrls.value = (
        await getSubscriptionTrackingUrls(topicName, subscriptionName)
      ).data;
    } catch (e) {
      error.value.getSubscriptionTrackingUrls = e as Error;
    }
  };

  const removeSubscription = async (): Promise<boolean> => {
    try {
      await deleteSubscription(topicName, subscriptionName);
      await notificationStore.dispatchNotification({
        text: useGlobalI18n().t('notifications.subscription.delete.success', {
          subscriptionName,
        }),
        type: 'success',
      });
      return true;
    } catch (e: any) {
      await dispatchErrorNotification(
        e,
        notificationStore,
        useGlobalI18n().t('notifications.subscription.delete.failure', {
          subscriptionName,
        }),
      );
      return false;
    }
  };

  const suspendSubscription = async (): Promise<boolean> => {
    try {
      await suspend(topicName, subscriptionName);
      await notificationStore.dispatchNotification({
        text: useGlobalI18n().t('notifications.subscription.suspend.success', {
          subscriptionName,
        }),
        type: 'success',
      });
      return true;
    } catch (e: any) {
      await dispatchErrorNotification(
        e,
        notificationStore,
        useGlobalI18n().t('notifications.subscription.suspend.failure', {
          subscriptionName,
        }),
      );
      return false;
    }
  };

  const activateSubscription = async (): Promise<boolean> => {
    try {
      await activate(topicName, subscriptionName);
      await notificationStore.dispatchNotification({
        text: useGlobalI18n().t('notifications.subscription.activate.success', {
          subscriptionName,
        }),
        type: 'success',
      });
      return true;
    } catch (e: any) {
      await dispatchErrorNotification(
        e,
        notificationStore,
        useGlobalI18n().t('notifications.subscription.activate.failure', {
          subscriptionName,
        }),
      );
      return false;
    }
  };

  const retransmitMessages = async (from: string): Promise<boolean> => {
    retransmitting.value = true;
    try {
      await retransmitSubscriptionMessages(topicName, subscriptionName, {
        retransmissionDate: from,
      });
      await notificationStore.dispatchNotification({
        title: useGlobalI18n().t(
          'notifications.subscription.retransmit.success',
          {
            subscriptionName,
          },
        ),
        text: '',
        type: 'success',
      });
      return true;
    } catch (e: any) {
      await dispatchErrorNotification(
        e,
        notificationStore,
        useGlobalI18n().t('notifications.subscription.retransmit.failure', {
          subscriptionName,
        }),
      );
      return false;
    } finally {
      retransmitting.value = false;
    }
  };

  const skipAllMessages = async (): Promise<boolean> => {
    skippingAllMessages.value = true;
    const tomorrowDate = new Date();
    tomorrowDate.setDate(tomorrowDate.getDate() + 1);
    try {
      await retransmitSubscriptionMessages(topicName, subscriptionName, {
        retransmissionDate: tomorrowDate.toISOString(),
      });
      await notificationStore.dispatchNotification({
        title: useGlobalI18n().t(
          'notifications.subscription.skipAllMessages.success',
          {
            subscriptionName,
          },
        ),
        text: '',
        type: 'success',
      });
      return true;
    } catch (e: any) {
      await dispatchErrorNotification(
        e,
        notificationStore,
        useGlobalI18n().t(
          'notifications.subscription.skipAllMessages.failure',
          {
            subscriptionName,
          },
        ),
      );
      return false;
    } finally {
      skippingAllMessages.value = false;
    }
  };

  fetchSubscription();
  fetchSubscriptionTrackingUrls();

  return {
    subscription,
    owner,
    subscriptionMetrics,
    subscriptionHealth,
    subscriptionUndeliveredMessages,
    subscriptionLastUndeliveredMessage,
    trackingUrls,
    loading,
    retransmitting,
    skippingAllMessages,
    error,
    removeSubscription,
    suspendSubscription,
    activateSubscription,
    retransmitMessages,
    skipAllMessages,
  };
}
