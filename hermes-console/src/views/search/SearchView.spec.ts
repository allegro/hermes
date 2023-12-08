import { computed, ref } from 'vue';
import { dummySubscription } from '@/dummy/subscription';
import { dummyTopic } from '@/dummy/topic';
import { expect } from 'vitest';
import { render } from '@/utils/test-utils';
import { useSearch } from '@/composables/search/useSearch';
import SearchView from '@/views/search/SearchView.vue';
import type { UseSearch } from '@/composables/search/useSearch';

vi.mock('@/composables/search/useSearch');

const useSearchStub: UseSearch = {
  topics: ref(),
  subscriptions: ref(),
  querySubscriptions: () => {},
  queryTopics: () => {},
  error: ref({
    fetchError: null,
  }),
  loading: ref(false),
};

describe('SearchView', () => {
  it('should render subscriptions table when subscriptions are present', () => {
    // given
    vi.mocked(useSearch).mockReturnValueOnce({
      ...useSearchStub,
      subscriptions: ref([dummySubscription]),
    });

    // when
    const { queryByTestId } = render(SearchView);
    // then
    expect(queryByTestId('subscription-search-results')).toBeVisible();
  });

  it('should render topics table when topics are present', () => {
    // given
    vi.mocked(useSearch).mockReturnValueOnce({
      ...useSearchStub,
      topics: ref([dummyTopic]),
    });

    // when
    const { queryByTestId } = render(SearchView);
    // then
    expect(queryByTestId('topic-search-results')).toBeVisible();
  });

  it('should show loading spinner when fetching search results', () => {
    // given
    vi.mocked(useSearch).mockReturnValueOnce({
      ...useSearchStub,
      loading: computed(() => true),
    });

    // when
    const { queryByTestId } = render(SearchView);

    // then
    expect(vi.mocked(useSearch)).toHaveBeenCalledOnce();
    expect(queryByTestId('loading-spinner')).toBeVisible();
  });

  it('should hide loading spinner when fetching search results is complete', () => {
    // given
    vi.mocked(useSearch).mockReturnValueOnce({
      ...useSearchStub,
      loading: computed(() => false),
    });

    // when
    const { queryByTestId } = render(SearchView);

    // then
    expect(vi.mocked(useSearch)).toHaveBeenCalledOnce();
    expect(queryByTestId('loading-spinner')).not.toBeInTheDocument();
  });

  it('should show error message when fetching search results failed', () => {
    // given
    vi.mocked(useSearch).mockReturnValueOnce({
      ...useSearchStub,
      loading: computed(() => false),
      error: ref({ fetchError: new Error() }),
    });

    // when
    const { queryByText } = render(SearchView);

    // then
    expect(vi.mocked(useSearch)).toHaveBeenCalledOnce();
    expect(queryByText('search.connectionError.title')).toBeVisible();
    expect(queryByText('search.connectionError.text')).toBeVisible();
  });

  it('should not show error message when fetching search results failed', () => {
    // given
    vi.mocked(useSearch).mockReturnValueOnce({
      ...useSearchStub,
      loading: computed(() => false),
      error: ref({ fetchError: null }),
    });

    // when
    const { queryByText } = render(SearchView);

    // then
    expect(vi.mocked(useSearch)).toHaveBeenCalledOnce();
    expect(queryByText('search.connectionError.title')).not.toBeInTheDocument();
    expect(queryByText('search.connectionError.text')).not.toBeInTheDocument();
  });
});
