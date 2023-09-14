import { useGlobalI18n } from '@/i18n';

export function dispatchAxiosErrorNotification(
  e: any,
  notificationStore: any,
  title: string,
) {
  const text = e.response?.data?.message
    ? e.response.data.message
    : useGlobalI18n().t('notifications.unknownError');
  notificationStore.dispatchNotification({
    title,
    text,
    type: 'error',
  });
}
