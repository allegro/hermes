import { dummyInconsistentTopics } from '@/dummy/inconsistentTopics';
import { ref } from 'vue';
import { render } from '@/utils/test-utils';
import { useInconsistentTopics } from '@/composables/inconsistent-topics/use-inconsistent-topics/useInconsistentTopics';
import ConsistencyView from '@/views/admin/consistency/ConsistencyView.vue';
import type { UseInconsistentTopics } from '@/composables/inconsistent-topics/use-inconsistent-topics/useInconsistentTopics';

vi.mock(
  '@/composables/inconsistent-topics/use-inconsistent-topics/useInconsistentTopics',
);

const useInconsistentTopicsStub: UseInconsistentTopics = {
  topics: ref(dummyInconsistentTopics),
  error: ref({
    fetchInconsistentTopics: null,
  }),
  loading: ref(false),
};

describe('ConsistencyView', () => {
  it('should render if datacenters consistency data was successfully fetched', () => {
    // given
    vi.mocked(useInconsistentTopics).mockReturnValueOnce(
      useInconsistentTopicsStub,
    );

    // when
    const { getByText } = render(ConsistencyView);

    // then
    expect(vi.mocked(useInconsistentTopics)).toHaveBeenCalledOnce();
    expect(getByText('consistency.inconsistentTopics.heading')).toBeVisible();
  });

  it('should show loading spinner when fetching Consistency data', () => {
    // given
    vi.mocked(useInconsistentTopics).mockReturnValueOnce({
      ...useInconsistentTopicsStub,
      loading: ref(true),
    });

    // when
    const { queryByTestId } = render(ConsistencyView);

    // then
    expect(vi.mocked(useInconsistentTopics)).toHaveBeenCalledOnce();
    expect(queryByTestId('loading-spinner')).toBeVisible();
  });

  it('should hide loading spinner when data fetch is complete', () => {
    // given
    vi.mocked(useInconsistentTopics).mockReturnValueOnce({
      ...useInconsistentTopicsStub,
      loading: ref(false),
    });

    // when
    const { queryByTestId } = render(ConsistencyView);

    // then
    expect(vi.mocked(useInconsistentTopics)).toHaveBeenCalledOnce();
    expect(queryByTestId('loading-spinner')).not.toBeInTheDocument();
  });

  it('should show error message when fetching data failed', () => {
    // given
    vi.mocked(useInconsistentTopics).mockReturnValueOnce({
      ...useInconsistentTopicsStub,
      loading: ref(false),
      error: ref({ fetchInconsistentTopics: new Error() }),
    });

    // when
    const { queryByText } = render(ConsistencyView);

    // then
    expect(vi.mocked(useInconsistentTopics)).toHaveBeenCalledOnce();
    expect(queryByText('consistency.connectionError.title')).toBeVisible();
    expect(queryByText('consistency.connectionError.text')).toBeVisible();
  });

  it('should not show error message when data was fetch successfully', () => {
    // given
    vi.mocked(useInconsistentTopics).mockReturnValueOnce({
      ...useInconsistentTopicsStub,
      loading: ref(false),
      error: ref({ fetchInconsistentTopics: null }),
    });

    // when
    const { queryByText } = render(ConsistencyView);

    // then
    expect(vi.mocked(useInconsistentTopics)).toHaveBeenCalledOnce();
    expect(
      queryByText('consistency.connectionError.title'),
    ).not.toBeInTheDocument();
  });
});
