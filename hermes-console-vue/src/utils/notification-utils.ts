import { useGlobalI18n } from '@/i18n';
import axios, { AxiosError } from 'axios';

export function dispatchErrorNotification(
  err: Error | AxiosError,
  notificationStore: any,
  title: string,
): void {
  let text = '';
  if (axios.isAxiosError(err)) {
    const e = err as AxiosError;
    // @ts-ignore
    text = e.response?.data?.message
      ? // @ts-ignore
        e.response.data.message
      : useGlobalI18n().t('notifications.unknownError');
  } else {
    text = err.message;
  }

  notificationStore.dispatchNotification({
    title,
    text,
    type: 'error',
  });
}
