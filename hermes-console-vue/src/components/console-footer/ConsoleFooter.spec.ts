import { createTestingPiniaWithState } from '@/dummy/store';
import { render } from '@/utils/test-utils';
import ConsoleFooter from '@/components/console-footer/ConsoleFooter.vue';

describe('ConsoleFooter', () => {
  it('renders properly', () => {
    // when
    const { getByText } = render(ConsoleFooter, {
      testPinia: createTestingPiniaWithState(),
    });
    // then
    expect(getByText(/allegro tech & contributors/i)).toBeVisible();
  });
});
