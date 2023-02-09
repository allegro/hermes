import { describe, expect, it } from 'vitest';
import { vuetifyRender } from '@/utils/test-utils';
import HomeMenu from '@/views/home/components/home-menu/HomeMenu.vue';

describe('HomeMenu', () => {
  it('renders properly', () => {
    const { getByText } = vuetifyRender(HomeMenu);
    expect(getByText(/menu/i)).toBeTruthy();
  });
});
