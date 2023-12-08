import { createTestingPinia } from '@pinia/testing';
import { expect } from 'vitest';
import { favoritesStoreState } from '@/dummy/store';
import { fireEvent, within } from '@testing-library/vue';
import { render } from '@/utils/test-utils';
import { setActivePinia } from 'pinia';
import { useFavorites } from '@/store/favorites/useFavorites';
import FavoriteTopicsListing from '@/views/favorite/topics/topic-listing/FavoriteTopicsListing.vue';

describe('FavoriteTopicsListing', () => {
  const dummyTopics = ['dummyEventV1', 'foobarEventV1'];

  it('should render listing with no filter applied', () => {
    // given
    const props = {
      topics: dummyTopics,
    };

    // when
    const { getByText } = render(FavoriteTopicsListing, { props });

    // then
    dummyTopics.forEach((name, index) => {
      const topicRow = getByText(name).closest('tr')!;
      expect(within(topicRow).getByText(`${index + 1}`)).toBeInTheDocument();
      expect(within(topicRow).getByText(name)).toBeInTheDocument();
    });
  });

  it.each(['fo', 'Fo', 'FO'])(
    'should render listing with a filter applied (case-insensitive, %s)',
    (filter: string) => {
      // given
      const props = {
        topics: dummyTopics,
        filter,
      };

      // when
      const topics = render(FavoriteTopicsListing, { props })
        .getAllByText(/foobarEventV1/)
        .map((topic) => topic.closest('tr'));

      // then
      expect(topics).toHaveLength(1);
    },
  );

  it('should render listing with a filter applied (no results)', () => {
    // given
    const props = {
      topics: dummyTopics,
      filter: 'notExisting',
    };

    // when
    const rows = render(FavoriteTopicsListing, { props }).getAllByRole('row');

    // then
    expect(rows).toHaveLength(2);
    expect(
      within(rows[0]!).getByText(/groups.groupTopicsListing.index/),
    ).toBeInTheDocument();
    expect(
      within(rows[0]!).getByText('groups.groupTopicsListing.name'),
    ).toBeInTheDocument();
    expect(
      within(rows[1]!).getByText(/groups.groupTopicsListing.noTopics/),
    ).toBeInTheDocument();
    expect(
      within(rows[1]!).getByText(/groups.groupTopicsListing.appliedFilter/),
    ).toBeInTheDocument();
  });

  it('should render an empty listing without filter applied', () => {
    // given
    const props = {
      topics: [],
    };

    // when
    const rows = render(FavoriteTopicsListing, { props }).getAllByRole('row');

    // then
    expect(rows).toHaveLength(2);
    expect(
      within(rows[1]!).getByText('groups.groupTopicsListing.noTopics'),
    ).toBeInTheDocument();
    expect(
      within(rows[1]!).queryByText('groups.groupTopicsListing.appliedFilter'),
    ).not.toBeInTheDocument();
  });

  it('should remove from favorites on button click', async () => {
    const pinia = createTestingPinia({
      createSpy: vi.fn,
      initialState: { favorites: favoritesStoreState },
    });
    setActivePinia(pinia);
    const favoritesStore = useFavorites();
    vi.mocked(favoritesStore.removeTopic).mockReturnValueOnce(
      Promise.resolve(),
    );
    const props = {
      topics: ['topicToRemove'],
    };

    // when
    const { getByRole } = render(FavoriteTopicsListing, {
      testPinia: pinia,
      props,
    });
    await fireEvent.click(getByRole('button'));

    // then
    expect(favoritesStore.removeTopic).toHaveBeenCalledWith('topicToRemove');
  });
});
