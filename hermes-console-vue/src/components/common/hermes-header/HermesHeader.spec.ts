import { describe, expect, it } from 'vitest';
import { vuetifyRender } from '@/utils/test-utils';
import HermesHeader from '@/components/common/hermes-header/HermesHeader.vue';

describe('HermesHeader', () => {
  it('renders properly', () => {
    const { getByText } = vuetifyRender(HermesHeader);
    expect(getByText(/hermes console/i)).toBeTruthy();
  });
});
