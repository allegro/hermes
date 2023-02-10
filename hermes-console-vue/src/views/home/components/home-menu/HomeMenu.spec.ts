import { vuetifyRender } from '@/utils/test-utils';
import HomeMenu from '@/views/home/components/home-menu/HomeMenu.vue';

describe('HomeMenu', () => {
  it('renders properly', () => {
    // when
    const { getByText } = vuetifyRender(HomeMenu);
    // then
    expect(getByText(/menu/i)).toBeTruthy();
  });
});
