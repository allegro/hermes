import '@testing-library/jest-dom';
import { useI18n } from 'vue-i18n';
import { vi } from 'vitest';

/*
 * Mock browser-specific elements.
 */
global.ResizeObserver = vi.fn(() => ({
  observe: vi.fn(),
  unobserve: vi.fn(),
  disconnect: vi.fn(),
}));

global.CSS = {
  escape: vi.fn(),
  supports: vi.fn(),
};

/*
 * Mock vue-i18n.
 */
vi.mock('vue-i18n');
vi.mocked(useI18n, {
  partial: true,
  deep: true,
}).mockReturnValue({
  t: (key) => key,
});
