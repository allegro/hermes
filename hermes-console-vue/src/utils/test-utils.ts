import { createVuetify } from 'vuetify';
import { h } from 'vue';
import type { RenderOptions, RenderResult } from '@testing-library/vue';
import { render as renderTL } from '@testing-library/vue';
import { VApp } from 'vuetify/components';
import router from '@/router';
import { createTestingPinia } from '@pinia/testing';
import type { Router } from 'vue-router';

type RenderParameters = {
  options?: Partial<RenderOptions>;
  props?: any;
  testVuetify?: ReturnType<typeof createVuetify>;
  testPinia?: ReturnType<typeof createTestingPinia>;
  testRouter?: Router;
};

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
