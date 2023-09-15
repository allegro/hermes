import { computed, ref } from 'vue';
import {
  deleteSubscriptionConstraint,
  deleteTopicConstraint,
  fetchConstraints as getConstraints,
  upsertSubscriptionConstraint,
  upsertTopicConstraint,
} from '@/api/hermes-client';
import { dispatchErrorNotification } from '@/utils/notification-utils';
import { parseSubscriptionFqn } from '@/utils/subscription-utils/subscription-utils';
import { useGlobalI18n } from '@/i18n';
import { useNotificationsStore } from '@/store/app-notifications/useAppNotifications';
import type { Constraint, ConstraintsConfig } from '@/api/constraints';
import type { Ref } from 'vue';

export interface UseConstraints {
  topicConstraints: Ref<Record<string, Constraint> | undefined>;
  subscriptionConstraints: Ref<Record<string, Constraint> | undefined>;
  upsertTopicConstraint: (
    topicName: string,
    constraint: Constraint,
  ) => Promise<boolean>;
  deleteTopicConstraint: (topicName: string) => Promise<boolean>;
  upsertSubscriptionConstraint: (
    subscriptionFqn: string,
    constraint: Constraint,
  ) => Promise<boolean>;
  deleteSubscriptionConstraint: (subscriptionFqn: string) => Promise<boolean>;
  loading: Ref<boolean>;
  error: Ref<UseConstraintsErrors>;
}

export interface UseConstraintsErrors {
  fetchConstraints: Error | null;
}

export function useConstraints(): UseConstraints {
  const constraints = ref<ConstraintsConfig>();
  const error = ref<UseConstraintsErrors>({
    fetchConstraints: null,
  });
  const loading = ref(false);

  const topicConstraints = computed((): Record<string, Constraint> => {
    return constraints.value?.topicConstraints ?? {};
  });

  const subscriptionConstraints = computed((): Record<string, Constraint> => {
    return constraints.value?.subscriptionConstraints ?? {};
  });

  const doUpsertTopicConstraint = async (
    topicName: string,
    constraint: Constraint,
  ): Promise<boolean> => {
    const notificationsStore = useNotificationsStore();

    try {
      await upsertTopicConstraint(topicName, constraint);

      await notificationsStore.dispatchNotification({
        title: useGlobalI18n().t(
          'notifications.constraints.topic.created.success',
          {
            topicName,
          },
        ),
        text: '',
        type: 'success',
      });
      return true;
    } catch (e: any) {
      dispatchErrorNotification(
        e,
        notificationsStore,
        useGlobalI18n().t('notifications.constraints.topic.created.failure', {
          topicName,
        }),
      );
      return false;
    }
  };

  const doDeleteTopicConstraint = async (topicName: string) => {
    const notificationsStore = useNotificationsStore();

    try {
      await deleteTopicConstraint(topicName);

      await notificationsStore.dispatchNotification({
        title: useGlobalI18n().t(
          'notifications.constraints.topic.deleted.success',
          {
            topicName,
          },
        ),
        text: '',
        type: 'success',
      });
      return true;
    } catch (e: any) {
      dispatchErrorNotification(
        e,
        notificationsStore,
        useGlobalI18n().t('notifications.constraints.topic.deleted.failure', {
          topicName,
        }),
      );
    }
    return false;
  };

  const doUpsertSubscriptionConstraint = async (
    subscriptionFqn: string,
    constraint: Constraint,
  ) => {
    const notificationsStore = useNotificationsStore();

    try {
      await upsertSubscriptionConstraint(subscriptionFqn, constraint);

      await notificationsStore.dispatchNotification({
        title: useGlobalI18n().t(
          'notifications.constraints.subscription.created.success',
          {
            subscriptionFqn,
          },
        ),
        text: '',
        type: 'success',
      });
      return true;
    } catch (e: any) {
      dispatchErrorNotification(
        e,
        notificationsStore,
        useGlobalI18n().t(
          'notifications.constraints.subscription.created.failure',
          {
            subscriptionFqn,
          },
        ),
      );
      return false;
    }
  };

  const doDeleteSubscriptionConstraint = async (subscriptionFqn: string) => {
    const notificationsStore = useNotificationsStore();

    try {
      const subscriptionName = parseSubscriptionFqn(subscriptionFqn);
      await deleteSubscriptionConstraint(
        subscriptionName.topicName,
        subscriptionName.subscriptionName,
      );

      await notificationsStore.dispatchNotification({
        title: useGlobalI18n().t(
          'notifications.constraints.subscription.deleted.success',
          {
            subscriptionFqn,
          },
        ),
        text: '',
        type: 'success',
      });
      return true;
    } catch (e: any) {
      dispatchErrorNotification(
        e,
        notificationsStore,
        useGlobalI18n().t(
          'notifications.constraints.subscription.deleted.failure',
          {
            subscriptionFqn,
          },
        ),
      );
      return false;
    }
  };

  const fetchConstraints = async () => {
    try {
      loading.value = true;
      constraints.value = (await getConstraints()).data;
    } catch (e) {
      error.value.fetchConstraints = e as Error;
    } finally {
      loading.value = false;
    }
  };

  fetchConstraints();

  return {
    topicConstraints: topicConstraints,
    subscriptionConstraints: subscriptionConstraints,
    upsertTopicConstraint: doUpsertTopicConstraint,
    deleteTopicConstraint: doDeleteTopicConstraint,
    upsertSubscriptionConstraint: doUpsertSubscriptionConstraint,
    deleteSubscriptionConstraint: doDeleteSubscriptionConstraint,
    loading,
    error,
  };
}
