import { createI18n } from 'vue-i18n';
import messages from '@/i18n/messages';

export const i18n = createI18n({
  legacy: false,
  locale: 'en-US',
  fallbackLocale: 'en-US',
  messages: messages,
});

export function useGlobalI18n() {
  return i18n.global;
}
