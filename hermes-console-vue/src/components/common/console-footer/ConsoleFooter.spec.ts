import { vuetifyRender } from '@/utils/test-utils';
import ConsoleFooter from '@/components/common/console-footer/ConsoleFooter.vue';

describe('ConsoleFooter', () => {
  it('renders properly', () => {
    // when
    const { getByText } = vuetifyRender(ConsoleFooter);
    // then
    expect(getByText(/allegro tech & contributors/i)).toBeTruthy();
  });
});
