import { createTestingPinia } from '@pinia/testing';
import { expect } from 'vitest';
import { favoritesStoreState } from '@/dummy/store';
import { fireEvent, within } from '@testing-library/vue';
import { render } from '@/utils/test-utils';
import { setActivePinia } from 'pinia';
import { useFavorites } from '@/store/favorites/useFavorites';
import FavoriteSubscriptionsListing from '@/views/favorite/subscriptions/subscription-listing/FavoriteSubscriptionsListing.vue';

describe('FavoriteSubscriptionsListing', () => {
  const dummySubscriptions = ['foo', 'bar'];

  it('should render listing with no filter applied', () => {
    // given
    const props = {
      subscriptions: dummySubscriptions,
    };

    // when
    const { getByText } = render(FavoriteSubscriptionsListing, { props });

    // then
    dummySubscriptions.forEach((name, index) => {
      const subscriptionRow = getByText(name).closest('tr')!;
      expect(
        within(subscriptionRow).getByText(`${index + 1}`),
      ).toBeInTheDocument();
      expect(within(subscriptionRow).getByText(name)).toBeInTheDocument();
    });
  });

  it('should render listing with a filter applied', () => {
    // given
    const props = {
      subscriptions: dummySubscriptions,
      filter: 'fo',
    };

    // when
    const subscriptions = render(FavoriteSubscriptionsListing, { props })
      .getAllByText(/foo/)
      .map((subscription) => subscription.closest('tr'));

    // then
    expect(subscriptions).toHaveLength(1);
  });

  it('should render listing with a filter applied (no results)', () => {
    // given
    const props = {
      subscriptions: dummySubscriptions,
      filter: 'DummySubV1',
    };

    // when
    const rows = render(FavoriteSubscriptionsListing, { props }).getAllByRole(
      'row',
    );

    // then
    expect(rows).toHaveLength(2);
    expect(
      within(rows[0]!).getByText(/favorites.subscriptions.index/),
    ).toBeInTheDocument();
    expect(
      within(rows[0]!).getByText('favorites.subscriptions.name'),
    ).toBeInTheDocument();
    expect(
      within(rows[1]!).getByText(/search.results.subscription.noSubscriptions/),
    ).toBeInTheDocument();
    expect(
      within(rows[1]!).getByText(/favorites.subscriptions.appliedFilter/),
    ).toBeInTheDocument();
  });

  it('should render an empty listing without filter applied', () => {
    // given
    const props = {
      subscriptions: [],
    };

    // when
    const rows = render(FavoriteSubscriptionsListing, { props }).getAllByRole(
      'row',
    );

    // then
    expect(rows).toHaveLength(2);
    expect(
      within(rows[1]!).getByText('search.results.subscription.noSubscriptions'),
    ).toBeInTheDocument();
    expect(
      within(rows[1]!).queryByText('favorites.subscriptions.appliedFilter'),
    ).not.toBeInTheDocument();
  });

  it('should remove from favorites on button click', async () => {
    const pinia = createTestingPinia({
      createSpy: vi.fn,
      initialState: { favorites: favoritesStoreState },
    });
    setActivePinia(pinia);
    const favoritesStore = useFavorites();
    vi.mocked(
      favoritesStore.removeSubscriptionByQualifiedName,
    ).mockReturnValueOnce(Promise.resolve());
    const props = {
      subscriptions: ['foobar'],
    };

    // when
    const { getByRole } = render(FavoriteSubscriptionsListing, {
      testPinia: pinia,
      props,
    });
    await fireEvent.click(getByRole('button'));

    // then
    expect(
      favoritesStore.removeSubscriptionByQualifiedName,
    ).toHaveBeenCalledWith('foobar');
  });
});
