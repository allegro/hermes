import { createTestingPiniaWithState } from '@/dummy/store';
import { expect } from 'vitest';
import { render } from '@/utils/test-utils';
import HomeView from '@/views/home/HomeView.vue';

describe('HomeView', () => {
  it('renders properly', () => {
    // when
    const { getByText } = render(HomeView);

    // then
    expect(getByText('homeView.links.console')).toBeVisible();
  });

  it.each([
    { text: 'homeView.links.console' },
    { text: 'homeView.links.runtime' },
    { text: 'homeView.links.statistics' },
    { text: 'homeView.links.search' },
    { text: 'homeView.links.documentation' },
    { text: 'homeView.links.adminTools' },
  ])('should render proper buttons', ({ text }) => {
    // when
    const { getByText } = render(HomeView, {
      testPinia: createTestingPiniaWithState(),
    });

    // then
    expect(getByText(text)).toBeVisible();
  });
});
