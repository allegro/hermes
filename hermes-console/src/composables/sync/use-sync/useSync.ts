import {
  syncGroup as doSyncGroup,
  syncSubscription as doSyncSubscription,
  syncTopic as doSyncTopic,
} from '@/api/hermes-client';
import { groupName } from '@/utils/topic-utils/topic-utils';
import { ref, type Ref } from 'vue';
import { useConsistencyStore } from '@/store/consistency/useConsistencyStore';
import { useGlobalI18n } from '@/i18n';
import { useNotificationsStore } from '@/store/app-notifications/useAppNotifications';

export interface UseSync {
  errorMessage: Ref<Error | null | undefined>;
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
  const errorMessage: Ref<Error | null | undefined> = ref();

  const notificationStore = useNotificationsStore();
  const consistencyStore = useConsistencyStore();

  const syncGroup = async (groupName: string, primaryDatacenter: string) => {
    try {
      await doSyncGroup(groupName, primaryDatacenter);
      await notificationStore.dispatchNotification({
        text: useGlobalI18n().t('notifications.sync.success', {
          group: groupName,
        }),
        type: 'success',
      });
      await consistencyStore.refresh(groupName);
      return true;
    } catch (e: any) {
      await notificationStore.dispatchNotification({
        text: useGlobalI18n().t('notifications.sync.failure', {
          group: groupName,
        }),
        type: 'error',
      });
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
        text: useGlobalI18n().t('notifications.sync.success', {
          group,
        }),
        type: 'success',
      });
      await consistencyStore.refresh(group);
      return true;
    } catch (e: any) {
      errorMessage.value = e as Error;
      await notificationStore.dispatchNotification({
        text: useGlobalI18n().t('notifications.sync.failure', {
          group,
        }),
        type: 'error',
      });
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
        text: useGlobalI18n().t('notifications.sync.success', {
          group,
        }),
        type: 'success',
      });
      await consistencyStore.refresh(group);
      return true;
    } catch (e) {
      errorMessage.value = e as Error;
      await notificationStore.dispatchNotification({
        text: useGlobalI18n().t('notifications.sync.failure', {
          group,
        }),
        type: 'error',
      });
      return false;
    }
  };

  return {
    errorMessage,
    syncGroup,
    syncSubscription,
    syncTopic,
  };
}
