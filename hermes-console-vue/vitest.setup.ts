import '@testing-library/jest-dom';
import { vi } from 'vitest';

global.ResizeObserver = vi.fn(() => ({
  observe: vi.fn(),
  unobserve: vi.fn(),
  disconnect: vi.fn(),
}));

global.CSS = {
  escape: vi.fn(),
  supports: vi.fn(),
};
