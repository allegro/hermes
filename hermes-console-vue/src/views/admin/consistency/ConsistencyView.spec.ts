import { computed, ref } from 'vue';
import { dummyInconsistentTopics } from '@/dummy/inconsistentTopics';
import { render } from '@/utils/test-utils';
import { useInconsistentTopics } from '@/composables/use-inconsistent-topics/useInconsistentTopics';
import ConsistencyView from '@/views/admin/consistency/ConsistencyView.vue';

vi.mock('@/composables/use-inconsistent-topics/useInconsistentTopics');

const useInconsistentTopicsStub: ReturnType<typeof useInconsistentTopics> = {
  topics: computed(() => dummyInconsistentTopics),
  error: ref(false),
  loading: computed(() => false),
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
    expect(
      getByText('consistency.inconsistentTopics.heading'),
    ).toBeInTheDocument();
  });

  it('should show loading spinner when fetching Consistency data', () => {
    // given
    vi.mocked(useInconsistentTopics).mockReturnValueOnce({
      ...useInconsistentTopicsStub,
      loading: computed(() => true),
    });

    // when
    const { queryByTestId } = render(ConsistencyView);

    // then
    expect(vi.mocked(useInconsistentTopics)).toHaveBeenCalledOnce();
    expect(queryByTestId('loading-spinner')).toBeInTheDocument();
  });

  it('should hide loading spinner when data fetch is complete', () => {
    // given
    vi.mocked(useInconsistentTopics).mockReturnValueOnce({
      ...useInconsistentTopicsStub,
      loading: computed(() => false),
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
      loading: computed(() => false),
      error: ref(true),
    });

    // when
    const { queryByText } = render(ConsistencyView);

    // then
    expect(vi.mocked(useInconsistentTopics)).toHaveBeenCalledOnce();
    expect(
      queryByText('consistency.connectionError.title'),
    ).toBeInTheDocument();
    expect(queryByText('consistency.connectionError.text')).toBeInTheDocument();
  });

  it('should not show error message when data was fetch successfully', () => {
    // given
    vi.mocked(useInconsistentTopics).mockReturnValueOnce({
      ...useInconsistentTopicsStub,
      loading: computed(() => false),
      error: ref(false),
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
