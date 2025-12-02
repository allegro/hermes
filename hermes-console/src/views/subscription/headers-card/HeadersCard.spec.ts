import { dummySubscription } from '@/dummy/subscription';
import { expect, it } from 'vitest';
import { render } from '@/utils/test-utils';
import { within } from '@testing-library/vue';
import HeadersCard from '@/views/subscription/headers-card/HeadersCard.vue';

describe('HeadersCard', () => {
  const props = {
    headers: dummySubscription.headers,
  };

  it('should render a card', () => {
    // when
    const { getByText } = render(HeadersCard, { props });

    // then
    expect(getByText('subscription.headersCard.title')).toBeVisible();
  });

  it('should render headers table', () => {
    // when
    const { getByText } = render(HeadersCard, { props });

    // then
    props.headers.forEach(({ name, value }, index) => {
      const row = getByText(index + 1).closest('tr')!;
      expect(within(row).getByText(index + 1)).toBeVisible();
      expect(within(row).getByText(name)).toBeVisible();
      expect(within(row).getByText(value)).toBeVisible();
    });
  });

  it('should display "no filters" message when no filters are defined', () => {
    // given
    const { getByText } = render(HeadersCard, {
      props: {
        headers: [],
      },
    });

    // then
    expect(getByText('subscription.headersCard.noHeaders')).toBeVisible();
  });
});
