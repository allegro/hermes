import { describe, expect, it } from 'vitest';
import { vuetifyRender } from '@/utils/test-utils';
import HomeView from '@/views/home/HomeView.vue';

describe('HomeView', () => {
  it('renders properly', () => {
    const { getByText } = vuetifyRender(HomeView);
    expect(getByText(/console/i)).toBeTruthy();
  });
});
