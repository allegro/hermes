import { dispatchErrorNotification } from '@/utils/notification-utils';
import {
  syncGroup as doSyncGroup,
  syncSubscription as doSyncSubscription,
  syncTopic as doSyncTopic,
} from '@/api/hermes-client';
import { groupName } from '@/utils/topic-utils/topic-utils';
import { useConsistencyStore } from '@/store/consistency/useConsistencyStore';
import { useGlobalI18n } from '@/i18n';
import { useNotificationsStore } from '@/store/app-notifications/useAppNotifications';

export interface UseSync {
  syncGroup: (groupName: string, primaryDatacenter: string) => Promise<boolean>;
  syncTopic: (
    topicQualifiedName: string,
    primaryDatacenter: string,
  ) => Promise<boolean>;
  syncSubscription: (
    topicQualifiedName: string,
    subscriptionName: string,
    primaryDatacenter: string,
  ) => Promise<boolean>;
}

export function useSync(): UseSync {
  const notificationStore = useNotificationsStore();
  const consistencyStore = useConsistencyStore();

  const syncGroup = async (groupName: string, primaryDatacenter: string) => {
    try {
      await doSyncGroup(groupName, primaryDatacenter);
      await notificationStore.dispatchNotification({
        text: useGlobalI18n().t('notifications.consistency.sync.success', {
          group: groupName,
        }),
        type: 'success',
      });
      await consistencyStore.refresh(groupName);
      return true;
    } catch (e: any) {
      dispatchErrorNotification(
        e,
        notificationStore,
        useGlobalI18n().t('notifications.consistency.sync.failure', {
          group: groupName,
        }),
      );
      return false;
    }
  };

  const syncTopic = async (
    topicQualifiedName: string,
    primaryDatacenter: string,
  ) => {
    const group = groupName(topicQualifiedName);
    try {
      await doSyncTopic(topicQualifiedName, primaryDatacenter);
      await notificationStore.dispatchNotification({
        text: useGlobalI18n().t('notifications.consistency.sync.success', {
          group,
        }),
        type: 'success',
      });
      await consistencyStore.refresh(group);
      return true;
    } catch (e: any) {
      dispatchErrorNotification(
        e,
        notificationStore,
        useGlobalI18n().t('notifications.consistency.sync.failure', {
          group,
        }),
      );
      return false;
    }
  };

  const syncSubscription = async (
    topicQualifiedName: string,
    subscriptionName: string,
    primaryDatacenter: string,
  ) => {
    const group = groupName(topicQualifiedName);
    try {
      await doSyncSubscription(
        topicQualifiedName,
        subscriptionName,
        primaryDatacenter,
      );
      await notificationStore.dispatchNotification({
        text: useGlobalI18n().t('notifications.consistency.sync.success', {
          group,
        }),
        type: 'success',
      });
      await consistencyStore.refresh(group);
      return true;
    } catch (e: any) {
      dispatchErrorNotification(
        e,
        notificationStore,
        useGlobalI18n().t('notifications.consistency.sync.failure', {
          group,
        }),
      );
      return false;
    }
  };

  return {
    syncGroup,
    syncSubscription,
    syncTopic,
  };
}
