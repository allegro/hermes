import { computed, ref } from 'vue';
import {
  removeGroup as deleteGroup,
  fetchGroupNames as getGroupNames,
} from '@/api/hermes-client';
import { fetchTopicNames as getTopicNames } from '@/api/hermes-client';
import { useNotificationsStore } from '@/store/app-notifications/useAppNotifications';
import i18n from '@/main';
import type { Ref } from 'vue';

export interface UseGroups {
  groups: Ref<Group[] | undefined>;
  loading: Ref<boolean>;
  error: Ref<UseGroupsErrors>;
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
        text: i18n.global.t('notifications.group.delete.success', {
          groupId,
        }),
        type: 'success',
      });
      return true;
    } catch (e) {
      notificationStore.dispatchNotification({
        title: i18n.global.t('notifications.group.delete.failure', {
          groupId,
        }),
        text: (e as Error).message,
        type: 'error',
      });
      return false;
    }
  };

  fetchGroupNames();
  fetchTopicNames();

  return {
    groups,
    loading,
    error,
    removeGroup,
  };
}
