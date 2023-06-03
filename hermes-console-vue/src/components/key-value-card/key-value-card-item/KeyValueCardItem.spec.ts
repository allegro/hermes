import { describe, expect } from 'vitest';
import { render } from '@/utils/test-utils';
import KeyValueCardItem from '@/components/key-value-card/key-value-card-item/KeyValueCardItem.vue';
import userEvent from '@testing-library/user-event';

describe('KeyValueCardItem', () => {
  it('should render component properly', () => {
    // given
    const props = {
      name: 'Sample key',
      value: 'Sample value',
    };

    // when
    const { getByText } = render(KeyValueCardItem, { props });

    // then
    const keyElement = getByText('Sample key');
    expect(keyElement).toBeInTheDocument();
    expect(keyElement).not.toHaveAttribute('href');

    // and
    const valueElement = getByText('Sample value');
    expect(valueElement).toBeInTheDocument();
  });

  it('should render component with url', () => {
    // given
    const props = {
      name: 'Sample key',
      nameHref: 'https://allegro.pl',
      value: 'Sample value',
    };

    // when
    const { getByText } = render(KeyValueCardItem, { props });

    // then
    const keyElement = getByText('Sample key');
    expect(keyElement).toBeInTheDocument();
    expect(keyElement).toHaveAttribute('href', 'https://allegro.pl');

    // and
    const valueElement = getByText('Sample value');
    expect(valueElement).toBeInTheDocument();
  });

  it('should render component with hidden tooltip', () => {
    // given
    const props = {
      name: 'Sample key',
      value: 'Sample value',
      tooltip: 'Sample tooltip text',
    };

    // when
    const { getByText } = render(KeyValueCardItem, { props });

    // then
    const keyElement = getByText('Sample key');
    expect(keyElement).toBeInTheDocument();

    // and
    const valueElement = getByText('Sample value');
    expect(valueElement).toBeInTheDocument();

    // and
    const tooltipElement = getByText('Sample tooltip text');
    expect(tooltipElement).toBeInTheDocument();
    expect(tooltipElement).not.toBeVisible();
  });

  it('should display tooltip text on mouse enter', async () => {
    // given
    const user = userEvent.setup();
    const props = {
      name: 'Sample key',
      value: 'Sample value',
      tooltip: 'Sample tooltip text',
    };
    const { getByText } = render(KeyValueCardItem, { props });
    const tooltipIcon = getByText('Sample tooltip text');

    // when
    await user.hover(tooltipIcon);

    // then
    expect(getByText('Sample tooltip text')).toBeVisible();
  });
});
