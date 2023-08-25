import {
  fetchReadiness as getReadiness,
  switchReadiness,
} from '@/api/hermes-client';
import { ref } from 'vue';
import { useNotificationsStore } from '@/store/app-notifications/useAppNotifications';
import i18n from '@/main';
import type { DatacenterReadiness } from '@/api/datacenter-readiness';
import type { Ref } from 'vue';

export interface UseReadiness {
  datacentersReadiness: Ref<DatacenterReadiness[] | undefined>;
  loading: Ref<boolean>;
  error: Ref<UseReadinessErrors>;
  switchReadinessState: (
    datacenter: string,
    desiredState: boolean,
  ) => Promise<boolean>;
}

export interface UseReadinessErrors {
  fetchReadiness: Error | null;
}

export function useReadiness(): UseReadiness {
  const notificationStore = useNotificationsStore();

  const datacentersReadiness = ref<DatacenterReadiness[]>();
  const error = ref<UseReadinessErrors>({
    fetchReadiness: null,
  });
  const loading = ref(false);

  const fetchReadiness = async () => {
    try {
      loading.value = true;
      datacentersReadiness.value = (await getReadiness()).data;
    } catch (e) {
      error.value.fetchReadiness = e as Error;
    } finally {
      loading.value = false;
    }
  };

  const switchReadinessState = async (
    datacenter: string,
    desiredState: boolean,
  ): Promise<boolean> => {
    try {
      await switchReadiness(datacenter, desiredState);
      notificationStore.dispatchNotification({
        text: i18n.global.t('notifications.readiness.switch.success', {
          datacenter,
        }),
        type: 'success',
      });
      return true;
    } catch (e) {
      notificationStore.dispatchNotification({
        title: i18n.global.t('notifications.readiness.switch.failure', {
          datacenter,
        }),
        text: (e as Error).message,
        type: 'error',
      });
      return false;
    }
  };

  fetchReadiness();

  return {
    datacentersReadiness,
    loading,
    error,
    switchReadinessState,
  };
}
