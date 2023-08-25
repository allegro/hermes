import { fetchRoles as getRoles } from '@/api/hermes-client';
import { ref } from 'vue';
import { useNotificationsStore } from '@/store/app-notifications/useAppNotifications';
import i18n from '@/main';
import type { Ref } from 'vue';
import type { Role } from '@/api/role';

export interface UseRoles {
  roles: Ref<Role[] | undefined>;
  loading: Ref<boolean>;
  error: Ref<UseRolesErrors>;
}

export interface UseRolesErrors {
  fetchRoles: Error | null;
}

export function useRoles(
  topicName: string | null,
  subscriptionName: string | null,
): UseRoles {
  const notificationStore = useNotificationsStore();

  const roles = ref<Role[]>();
  const error = ref<UseRolesErrors>({
    fetchRoles: null,
  });
  const loading = ref(false);
  const fetchRoles = async () => {
    try {
      loading.value = true;
      roles.value = (
        await getRoles(buildPath(topicName, subscriptionName))
      ).data;
    } catch (e) {
      error.value.fetchRoles = e as Error;
      notificationStore.dispatchNotification({
        text: i18n.global.t('notifications.roles.fetch.failure'),
        type: 'warning',
      });
    } finally {
      loading.value = false;
    }
  };

  fetchRoles();

  return {
    roles,
    loading,
    error,
  };
}

function buildPath(
  topicName: string | null,
  subscriptionName: string | null,
): string {
  if (topicName && subscriptionName) {
    return `/roles/topics/${topicName}/subscriptions/${subscriptionName}`;
  } else if (topicName) {
    return `/roles/topics/${topicName}`;
  }
  return '/roles';
}
