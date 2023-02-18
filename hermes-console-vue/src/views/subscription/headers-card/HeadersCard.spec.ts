import { dummySubscription } from '@/dummy/subscription';
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
    expect(getByText('Fixed HTTP headers')).toBeInTheDocument();
  });

  it('should render headers table', () => {
    // when
    const { getByText } = render(HeadersCard, { props });

    // then
    props.headers.forEach(({ name, value }, index) => {
      const row = getByText(index + 1).closest('tr')!;
      expect(within(row).getByText(index + 1)).toBeInTheDocument();
      expect(within(row).getByText(name)).toBeInTheDocument();
      expect(within(row).getByText(value)).toBeInTheDocument();
    });
  });
});
