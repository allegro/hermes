import { computed, ref } from 'vue';
import {
  createGroup,
  removeGroup as deleteGroup,
  fetchGroupNames as getGroupNames,
} from '@/api/hermes-client';
import { dispatchErrorNotification } from '@/utils/notification-utils';
import { fetchTopicNames as getTopicNames } from '@/api/hermes-client';
import { useGlobalI18n } from '@/i18n';
import { useNotificationsStore } from '@/store/app-notifications/useAppNotifications';
import type { Ref } from 'vue';

export interface UseGroups {
  groups: Ref<Group[] | undefined>;
  loading: Ref<boolean>;
  error: Ref<UseGroupsErrors>;
  createGroup: (groupId: string) => Promise<boolean>;
  removeGroup: (groupId: string) => Promise<boolean>;
}

export interface UseGroupsErrors {
  fetchGroupNames: Error | null;
  fetchTopicNames: Error | null;
}

export interface Group {
  name: string;
  topics: string[];
}

export function useGroups(): UseGroups {
  const notificationStore = useNotificationsStore();

  const groupNames = ref<string[]>();
  const topicNames = ref<string[]>();
  const error = ref<UseGroupsErrors>({
    fetchGroupNames: null,
    fetchTopicNames: null,
  });
  const loading = ref(false);

  const groups = computed((): Group[] | undefined => {
    return groupNames.value
      ?.map((groupName) => ({
        name: groupName,
        topics:
          topicNames.value?.filter(
            (topicName) =>
              topicName.indexOf(groupName) === 0 &&
              groupName.length === topicName.lastIndexOf('.'),
          ) ?? [],
      }))
      .sort(({ name: a }, { name: b }) => a.localeCompare(b));
  });

  const fetchGroupNames = async () => {
    try {
      loading.value = true;
      groupNames.value = (await getGroupNames()).data;
    } catch (e) {
      error.value.fetchGroupNames = e as Error;
    } finally {
      loading.value = false;
    }
  };

  const fetchTopicNames = async () => {
    try {
      loading.value = true;
      topicNames.value = (await getTopicNames()).data;
    } catch (e) {
      error.value.fetchTopicNames = e as Error;
    } finally {
      loading.value = false;
    }
  };

  const removeGroup = async (groupId: string): Promise<boolean> => {
    try {
      await deleteGroup(groupId);
      notificationStore.dispatchNotification({
        text: useGlobalI18n().t('notifications.group.delete.success', {
          groupId,
        }),
        type: 'success',
      });
      return true;
    } catch (e: any) {
      dispatchErrorNotification(
        e,
        notificationStore,
        useGlobalI18n().t('notifications.group.delete.failure', {
          groupId,
        }),
      );
      return false;
    }
  };

  const doCreateGroup = async (groupId: string): Promise<boolean> => {
    try {
      await createGroup({ groupName: groupId });
      notificationStore.dispatchNotification({
        title: useGlobalI18n().t('notifications.group.create.success', {
          groupId,
        }),
        text: '',
        type: 'success',
      });
      return true;
    } catch (e: any) {
      dispatchErrorNotification(
        e,
        notificationStore,
        useGlobalI18n().t('notifications.group.create.failure', {
          groupId,
        }),
      );
      return false;
    }
  };

  fetchGroupNames();
  fetchTopicNames();

  return {
    groups,
    loading,
    error,
    createGroup: doCreateGroup,
    removeGroup,
  };
}
