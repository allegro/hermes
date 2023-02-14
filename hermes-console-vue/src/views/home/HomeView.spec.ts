import { vuetifyRender } from '@/utils/test-utils';
import HomeView from '@/views/home/HomeView.vue';

describe('HomeView', () => {
  it('renders properly', () => {
    // when
    const { getByText } = vuetifyRender(HomeView);

    // then
    expect(getByText(/console/i)).toBeInTheDocument();
  });
});
