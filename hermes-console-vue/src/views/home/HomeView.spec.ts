import { render } from '@/utils/test-utils';
import HomeView from '@/views/home/HomeView.vue';

describe('HomeView', () => {
  it('renders properly', () => {
    // when
    const { getByText } = render(HomeView);

    // then
    expect(getByText(/console/i)).toBeVisible();
  });
});
