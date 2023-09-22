import { dispatchErrorNotification } from '@/utils/notification-utils';
import {
  fetchReadiness as getReadiness,
  switchReadiness,
} from '@/api/hermes-client';
import { ref } from 'vue';
import { useGlobalI18n } from '@/i18n';
import { useNotificationsStore } from '@/store/app-notifications/useAppNotifications';
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
      await switchReadiness(datacenter, {
        isReady: desiredState,
      });
      notificationStore.dispatchNotification({
        text: useGlobalI18n().t('notifications.readiness.switch.success', {
          datacenter,
        }),
        type: 'success',
      });
      return true;
    } catch (e: any) {
      dispatchErrorNotification(
        e,
        notificationStore,
        useGlobalI18n().t('notifications.readiness.switch.failure', {
          datacenter,
        }),
      );
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
