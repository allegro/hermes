import { createTestingPinia } from '@pinia/testing';
import { createTestingPiniaWithState } from '@/dummy/store';
import { render } from '@/utils/test-utils';
import FavoriteSubscriptionsView from '@/views/favorite/subscriptions/FavoriteSubscriptionsView.vue';

vi.mock('@/composables/groups/use-groups/useGroups');
vi.mock('@/composables/roles/use-roles/useRoles');

describe('FavoriteSubscriptionsView', () => {
  it('should render', () => {
    // when
    const { getByText } = render(FavoriteSubscriptionsView, {
      testPinia: createTestingPiniaWithState(),
    });

    // then
    expect(getByText('favorites.subscriptions.heading')).toBeVisible();
    expect(getByText('favorites.subscriptions.actions.search')).toBeVisible();
  });

  it('should render with subscription listing', async () => {
    // when
    const { getByText } = render(FavoriteSubscriptionsView, {
      testPinia: createTestingPinia({
        initialState: {
          favorites: {
            subscriptions: ['dummySubscription', 'foobarSubscription'],
          },
        },
      }),
    });

    // then
    expect(getByText('dummySubscription')).toBeInTheDocument();
    expect(getByText('foobarSubscription')).toBeInTheDocument();
  });
});
