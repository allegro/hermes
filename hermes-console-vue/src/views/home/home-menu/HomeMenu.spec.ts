import { render } from '@/utils/test-utils';
import HomeMenu from '@/views/home/home-menu/HomeMenu.vue';

describe('HomeMenu', () => {
  it('renders properly', () => {
    // when
    const { getByText } = render(HomeMenu);
    // then
    expect(getByText(/menu/i)).toBeInTheDocument();
  });
});
