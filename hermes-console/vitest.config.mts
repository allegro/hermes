import { defineConfig } from 'vitest/config';
import { fileURLToPath, URL } from 'node:url';
import AllureReporter from 'allure-vitest/reporter';
import vue from '@vitejs/plugin-vue';
import vuetify from 'vite-plugin-vuetify';

export default defineConfig({
  plugins: [vue() as any, vuetify({ autoImport: true })],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  test: {
    clearMocks: true,
    deps: {
      inline: ['vuetify'],
    },
    environment: 'jsdom',
    globals: true,
    setupFiles: ['vitest.setup.ts', 'allure-vitest/setup'],
    reporters: ['default', new AllureReporter({})],
  },
});
