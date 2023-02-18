import { dummySubscription } from '@/dummy/subscription';
import { render } from '@/utils/test-utils';
import { within } from '@testing-library/vue';
import FiltersCard from '@/views/subscription/filters-card/FiltersCard.vue';

describe('FiltersCard', () => {
  const props = {
    filters: dummySubscription.filters,
  };

  it('should render a card', () => {
    // when
    const { getByText } = render(FiltersCard, { props });

    // then
    expect(getByText('Subscription message filters')).toBeInTheDocument();
  });

  it('should render filters table', () => {
    // when
    const { getByText } = render(FiltersCard, { props });

    // then
    props.filters.forEach(
      ({ type, path, matcher, matchingStrategy }, index) => {
        const row = getByText(index + 1).closest('tr')!;
        expect(within(row).getByText(index + 1)).toBeInTheDocument();
        expect(within(row).getByText(type)).toBeInTheDocument();
        expect(within(row).getByText(path)).toBeInTheDocument();
        expect(within(row).getByText(matcher)).toBeInTheDocument();
        expect(within(row).getByText(matchingStrategy)).toBeInTheDocument();
      },
    );
  });
});
