import { createTestingPinia } from '@pinia/testing';
import { createTestingPiniaWithState } from '@/dummy/store';
import { render } from '@/utils/test-utils';
import FavoriteTopicsView from '@/views/favorite/topics/FavoriteTopicsView.vue';

vi.mock('@/composables/groups/use-groups/useGroups');
vi.mock('@/composables/roles/use-roles/useRoles');

describe('FavoriteTopicsView', () => {
  it('should render', () => {
    // when
    const { getByText } = render(FavoriteTopicsView, {
      testPinia: createTestingPiniaWithState(),
    });

    // then
    expect(getByText('favorites.topics.heading')).toBeVisible();
    expect(getByText('favorites.topics.actions.search')).toBeVisible();
  });

  it('should render with topic listing', async () => {
    // when
    const { getByText } = render(FavoriteTopicsView, {
      testPinia: createTestingPinia({
        initialState: {
          favorites: {
            topics: ['dummyTopic', 'foobarTopic'],
          },
        },
      }),
    });

    // then
    expect(getByText('dummyTopic')).toBeInTheDocument();
    expect(getByText('foobarTopic')).toBeInTheDocument();
  });
});
