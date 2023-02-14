import { createVuetify } from 'vuetify';
import { h } from 'vue';
import { render } from '@testing-library/vue';
import { VApp } from 'vuetify/components';
import router from '@/router';
import type { RenderOptions } from '@testing-library/vue';
import type { Router } from 'vue-router';

export const vuetifyRender = (
  TestComponent: any,
  options?: Partial<RenderOptions>,
  testVuetify: ReturnType<typeof createVuetify> = createVuetify(),
  testRouter: Router = router,
) => {
  return render(VApp, {
    global: {
      plugins: [testVuetify, testRouter],
    },
    slots: {
      default: h(TestComponent, options?.props),
    },
    ...options,
  });
};
