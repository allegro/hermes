import { dispatchErrorNotification } from '@/utils/notification-utils';
import { ref } from 'vue';
import { useGlobalI18n } from '@/i18n';
import { useNotificationsStore } from '@/store/app-notifications/useAppNotifications';
import { VerificationStatus } from '@/api/message-filters-verification';
import { verifyFilters } from '@/api/hermes-client';
import type { PathFilter } from '@/views/subscription/subscription-form/subscription-basic-filters/types';
import type { Ref } from 'vue';

export interface UseSubscriptionFiltersDebug {
  status: Ref<VerificationStatus | undefined>;
  errorMessage: Ref<string | undefined>;
  verify: (
    topicName: string,
    filters: PathFilter[],
    message: string,
  ) => Promise<void>;
}

export function useSubscriptionFiltersDebug(): UseSubscriptionFiltersDebug {
  const notificationStore = useNotificationsStore();
  const status = ref();
  const errorMessage = ref();
  const verify = async (
    topicName: string,
    filters: PathFilter[],
    message: string,
  ) => {
    try {
      const response = (
        await verifyFilters(topicName, {
          message: btoa(message),
          filters: filters,
        })
      ).data;
      if (response.status == VerificationStatus.ERROR) {
        errorMessage.value = response.errorMessage;
      }
      status.value = response.status;
    } catch (e: any) {
      dispatchErrorNotification(
        e,
        notificationStore,
        useGlobalI18n().t('notifications.subscriptionFiltersDebug.failure', {
          topicName,
        }),
      );
    }
  };
  return {
    status,
    errorMessage,
    verify,
  };
}
