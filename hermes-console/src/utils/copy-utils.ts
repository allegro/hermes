import { useGlobalI18n } from '@/i18n';
import { useNotificationsStore } from '@/store/app-notifications/useAppNotifications';

export const copyToClipboard = (content: string) => {
  if (window.isSecureContext && navigator.clipboard) {
    navigator.clipboard.writeText(content);
    dispatchSuccessCopyNotification();
  } else {
    unsecuredCopyToClipboard(content);
  }
};

function unsecuredCopyToClipboard(text: string) {
  const textArea = document.createElement('textarea');
  textArea.value = text;
  document.body.appendChild(textArea);
  textArea.focus();
  textArea.select();
  try {
    document.execCommand('copy');
    dispatchSuccessCopyNotification();
  } catch (err) {
    console.error('Unable to copy to clipboard', err);
    dispatchErrorCopyNotification(err);
  }
  document.body.removeChild(textArea);
}

function dispatchSuccessCopyNotification() {
  const notificationsStore = useNotificationsStore();
  notificationsStore.dispatchNotification({
    text: useGlobalI18n().t('notifications.copy.success'),
    type: 'success',
  });
}

function dispatchErrorCopyNotification(message: any) {
  const notificationsStore = useNotificationsStore();
  notificationsStore.dispatchNotification({
    title: useGlobalI18n().t('notifications.copy.error'),
    text: message,
    type: 'error',
  });
}
