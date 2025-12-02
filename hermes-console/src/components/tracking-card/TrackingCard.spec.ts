import { expect } from 'vitest';
import { render } from '@/utils/test-utils';
import TrackingCard from './TrackingCard.vue';

describe('TrackingCard', () => {
  const props = {
    trackingUrls: [
      { name: 'url1', url: 'https://test-tracking-url1' },
      { name: 'url2', url: 'https://test-tracking-url2' },
    ],
  };

  it('should render title properly', () => {
    // when
    const { getByText } = render(TrackingCard, { props });

    // then
    const row = getByText('trackingCard.title');
    expect(row).toBeVisible();
  });

  it('should render all tracking urls', () => {
    // when
    const { container } = render(TrackingCard, { props });

    // then
    const elements = container.querySelectorAll('a')!!;

    // first list item has link
    expect(elements[0]).toHaveAttribute('href', 'https://test-tracking-url1');
    expect(elements[0]).toHaveTextContent('url1');

    // link inside the first list item has link
    expect(elements[1]).toHaveTextContent('url1');
    expect(elements[1]).toHaveAttribute('href', 'https://test-tracking-url1');

    // first list item has link
    expect(elements[2]).toHaveAttribute('href', 'https://test-tracking-url2');
    expect(elements[2]).toHaveTextContent('url2');

    // link inside the second list item has link
    expect(elements[3]).toHaveTextContent('url2');
    expect(elements[3]).toHaveAttribute('href', 'https://test-tracking-url2');
  });

  it('should render message when no tracking urls', () => {
    // given
    const emptyProps = { trackingUrls: [] };
    const { getByText } = render(TrackingCard, { props: emptyProps });

    // then
    const row = getByText('trackingCard.noTrackingUrls');
    expect(row).toBeVisible();
  });
});
