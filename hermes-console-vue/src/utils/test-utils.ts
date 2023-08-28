import { createTestingPinia } from '@pinia/testing';
import { createVuetify } from 'vuetify';
import { expect } from 'vitest';
import { h } from 'vue';
import { render as renderTL } from '@testing-library/vue';
import { useNotificationsStore } from '@/store/app-notifications/useAppNotifications';
import { VApp } from 'vuetify/components';
import router from '@/router';
import type { NotificationConfig } from '@/store/app-notifications/types';
import type { RenderOptions, RenderResult } from '@testing-library/vue';
import type { Router } from 'vue-router';
import type { SpyInstance } from 'vitest';

type RenderParameters = {
  options?: Partial<RenderOptions>;
  props?: any;
  testVuetify?: ReturnType<typeof createVuetify>;
  testPinia?: ReturnType<typeof createTestingPinia>;
  testRouter?: Router;
};

export function notificationStoreSpy(): SpyInstance<
  [NotificationConfig],
  Promise<void>
> {
  const notificationsStore = useNotificationsStore();
  return vi.spyOn(notificationsStore, 'dispatchNotification');
}

export function expectNotificationDispatched(
  dispatcher: SpyInstance<[NotificationConfig], Promise<void>>,
  match: {
    type?: string;
    title?: string;
    text?: string;
  },
) {
  expect(dispatcher).toHaveBeenLastCalledWith(expect.objectContaining(match));
}

export const render = (
  TestComponent: any,
  {
    options = undefined,
    props = undefined,
    testVuetify = createVuetify(),
    testPinia = createTestingPinia(),
    testRouter = router,
  }: RenderParameters = {
    testVuetify: createVuetify(),
    testPinia: createTestingPinia(),
    testRouter: router,
  },
): RenderResult => {
  if (!options?.props) {
    options = { ...options, props };
  }
  return renderTL(VApp, {
    global: {
      plugins: [testVuetify, testRouter, testPinia],
      mocks: {
        $t: (key: string) => key,
      },
    },
    slots: {
      default: h(TestComponent, options?.props),
    },
    ...options,
  });
};
