import { describe, expect } from 'vitest';
import { h } from 'vue';
import { render } from '@/utils/test-utils';
import KeyValueCard from '@/components/key-value-card/KeyValueCard.vue';

describe('KeyValueCard', () => {
  const Component = h(KeyValueCard, { title: 'Sample title' }, () => [
    h('span', 'Hello'),
    h('span', 'World!'),
  ]);

  it('should render title properly', () => {
    // given
    const { getByText } = render(Component);

    // expect
    expect(getByText('Sample title')).toBeVisible();
  });

  it('should render body', () => {
    // given
    const { getByText, getByRole } = render(Component);

    // expect
    const table = getByRole('table');
    expect(table).to.exist;
    expect(getByText('Hello')).toBeVisible();
    expect(getByText('World!')).toBeVisible();
  });
});
