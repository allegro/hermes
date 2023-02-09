import { createVuetify } from 'vuetify';
import { h } from 'vue';
import { render } from '@testing-library/vue';
import { VApp } from 'vuetify/components';
import type { RenderOptions } from '@testing-library/vue';

export const vuetifyRender = (TestComponent: any, options?: RenderOptions) => {
  return render(VApp, {
    ...options,
    global: {
      plugins: [createVuetify()],
    },
    slots: {
      default: h(TestComponent),
    },
  });
};
