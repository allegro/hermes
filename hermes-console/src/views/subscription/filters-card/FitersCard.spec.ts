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
    expect(getByText('subscription.filtersCard.title')).toBeVisible();
  });

  it('should render filters table', () => {
    // when
    const { getByText } = render(FiltersCard, { props });

    // then
    props.filters.forEach(
      ({ type, path, matcher, matchingStrategy }, index) => {
        const row = getByText(index + 1).closest('tr')!;
        expect(within(row).getByText(index + 1)).toBeVisible();
        expect(within(row).getByText(type)).toBeVisible();
        expect(within(row).getByText(matcher)).toBeVisible();

        if (type !== 'header') {
          expect(within(row).getByText(path)).toBeVisible();
          expect(within(row).getByText(matchingStrategy)).toBeVisible();
        }
      },
    );
  });
});
