import { fetchRoles as getRoles } from '@/api/hermes-client';
import { type MaybeRef, ref, toValue, watch } from 'vue';
import { useGlobalI18n } from '@/i18n';
import { useNotificationsStore } from '@/store/app-notifications/useAppNotifications';
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
  topicName: MaybeRef<string | null>,
  subscriptionName: MaybeRef<string | null>,
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
        await getRoles(buildPath(toValue(topicName), toValue(subscriptionName)))
      ).data;
    } catch (e) {
      error.value.fetchRoles = e as Error;
      await notificationStore.dispatchNotification({
        text: useGlobalI18n().t('notifications.roles.fetch.failure'),
        type: 'warning',
      });
    } finally {
      loading.value = false;
    }
  };

  watch(
    () => [toValue(topicName), toValue(subscriptionName)],
    async () => {
      await fetchRoles();
    },
    { immediate: true },
  );

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
