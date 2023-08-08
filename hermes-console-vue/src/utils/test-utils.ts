import { createVuetify } from 'vuetify';
import { h } from 'vue';
import { render as renderTL } from '@testing-library/vue';
import { VApp } from 'vuetify/components';
import router from '@/router';
import type { RenderOptions, RenderResult } from '@testing-library/vue';
import type { Router } from 'vue-router';

export const render = (
  TestComponent: any,
  options?: Partial<RenderOptions>,
  testVuetify: ReturnType<typeof createVuetify> = createVuetify(),
  testRouter: Router = router,
): RenderResult => {
  return renderTL(VApp, {
    global: {
      plugins: [testVuetify, testRouter],
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
