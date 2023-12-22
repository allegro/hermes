import { fetchDashboardUrl as getDashboardUrl } from '@/api/hermes-client';
import { ref } from 'vue';
import { useGlobalI18n } from '@/i18n';
import { useNotificationsStore } from '@/store/app-notifications/useAppNotifications';
import type { Ref } from 'vue';

export interface UseMetrics {
  dashboardUrl: Ref<string>;
  loading: Ref<boolean>;
  error: Ref<UseMetricsErrors>;
}

export interface DashboardUrl {
  url: string;
}

export interface UseMetricsErrors {
  fetchDashboardUrl: Error | null;
}

export function useMetrics(
  topicName: string,
  subscriptionName: string | null,
): UseMetrics {
  const notificationStore = useNotificationsStore();

  const dashboardUrl = ref<string>('');
  const error = ref<UseMetricsErrors>({
    fetchDashboardUrl: null,
  });
  const loading = ref(false);
  const fetchDashboardUrl = async () => {
    try {
      loading.value = true;
      dashboardUrl.value = (
        await getDashboardUrl(buildPath(topicName, subscriptionName))
      ).data.url;
    } catch (e) {
      error.value.fetchDashboardUrl = e as Error;
      await notificationStore.dispatchNotification({
        text: useGlobalI18n().t('notifications.dashboardUrl.error'),
        type: 'warning',
      });
    } finally {
      loading.value = false;
    }
  };

  fetchDashboardUrl();

  return {
    dashboardUrl,
    loading,
    error,
  };
}

function buildPath(topicName: string, subscriptionName: string | null): string {
  if (subscriptionName) {
    return `/dashboards/topics/${topicName}/subscriptions/${subscriptionName}`;
  }
  return `/dashboards/topics/${topicName}`;
}
