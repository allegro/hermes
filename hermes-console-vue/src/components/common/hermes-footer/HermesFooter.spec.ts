import { describe, expect, it } from 'vitest';
import { vuetifyRender } from '@/utils/test-utils';
import HermesFooter from '@/components/common/hermes-footer/HermesFooter.vue';

describe('HermesFooter', () => {
  it('renders properly', () => {
    const { findByText } = vuetifyRender(HermesFooter);
    expect(findByText(/allegro tech and contributors/i)).toBeTruthy();
  });
});
