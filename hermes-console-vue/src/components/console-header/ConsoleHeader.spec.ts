import { render } from '@/utils/test-utils';
import ConsoleHeader from '@/components/console-header/ConsoleHeader.vue';

describe('ConsoleHeader', () => {
  it('renders properly', () => {
    // when
    const { getByRole } = render(ConsoleHeader);

    // then
    expect(getByRole('img')).toHaveAttribute('alt', 'Hermes');
  });
});
