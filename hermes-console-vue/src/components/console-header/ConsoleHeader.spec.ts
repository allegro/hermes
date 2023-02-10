import { vuetifyRender } from '@/utils/test-utils';
import ConsoleHeader from '@/components/console-header/ConsoleHeader.vue';

describe('ConsoleHeader', () => {
  it('renders properly', () => {
    // when
    const { getByText } = vuetifyRender(ConsoleHeader);
    // then
    expect(getByText(/hermes console/i)).toBeTruthy();
  });
});
