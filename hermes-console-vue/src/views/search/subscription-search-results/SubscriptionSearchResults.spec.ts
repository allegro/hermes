import { describe } from 'vitest';
import {
  dummySubscription,
  secondDummySubscription,
} from '@/dummy/subscription';
import { render } from '@/utils/test-utils';
import { within } from '@testing-library/vue';
import SubscriptionSearchResults from '@/views/search/subscription-search-results/SubscriptionSearchResults.vue';

describe('SubscriptionSearchResults', () => {
  const props = {
    subscriptions: [dummySubscription, secondDummySubscription],
  };

  it('should render subscriptions table', () => {
    // when
    const rows = render(SubscriptionSearchResults, { props }).getAllByRole(
      'row',
    );
    expect(rows).toHaveLength(3);
    expect(
      within(rows[0]!).getByText('search.results.subscription.name'),
    ).toBeVisible();
    expect(
      within(rows[0]!).getByText('search.results.subscription.endpoint'),
    ).toBeVisible();
    expect(
      within(rows[0]!).getByText('search.results.subscription.owner'),
    ).toBeVisible();
    expect(
      within(rows[0]!).getByText('search.results.subscription.status'),
    ).toBeVisible();

    props.subscriptions.forEach((subscription, index) => {
      expect(
        within(rows[index + 1]).getByText(subscription.name),
      ).toBeVisible();
    });
  });
});
