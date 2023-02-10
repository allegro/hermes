import { createVuetify } from 'vuetify';
import { h } from 'vue';
import { render } from '@testing-library/vue';
import { VApp } from 'vuetify/components';
import type { RenderOptions } from '@testing-library/vue';

export const vuetifyRender = (
  TestComponent: any,
  options?: Partial<RenderOptions>,
  vuetify?: ReturnType<typeof createVuetify>,
) => {
  return render(VApp, {
    global: {
      plugins: [vuetify ?? createVuetify()],
    },
    slots: {
      default: h(TestComponent, options?.props),
    },
    ...options,
  });
};
