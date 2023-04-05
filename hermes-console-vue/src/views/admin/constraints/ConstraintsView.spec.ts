import { computed, ref } from 'vue';
import { dummyConstraints } from '@/dummy/constraints';
import { render } from '@/utils/test-utils';
import { useConstraints } from '@/composables/use-constraints/useConstraints';
import ConstraintsView from '@/views/admin/constraints/ConstraintsView.vue';

vi.mock('@/composables/use-constraints/useConstraints');

const useConstraintsStub: ReturnType<typeof useConstraints> = {
  topicConstraints: computed(() => dummyConstraints.topicConstraints),
  subscriptionConstraints: computed(
    () => dummyConstraints.subscriptionConstraints,
  ),
  loading: computed(() => false),
  error: ref(false),
};

describe('ConstraintsView', () => {
  it('should render if constraints data was successfully fetched', () => {
    // given
    vi.mocked(useConstraints).mockReturnValueOnce(useConstraintsStub);

    // when
    const { getByText } = render(ConstraintsView);

    // then
    expect(vi.mocked(useConstraints)).toHaveBeenCalledOnce();
    expect(
      getByText('constraints.topicConstraints.heading'),
    ).toBeInTheDocument();
    expect(
      getByText('constraints.subscriptionConstraints.heading'),
    ).toBeInTheDocument();
  });

  it('should show loading spinner when fetching Constraints data', () => {
    // given
    vi.mocked(useConstraints).mockReturnValueOnce({
      ...useConstraintsStub,
      loading: computed(() => true),
    });

    // when
    const { queryByTestId } = render(ConstraintsView);

    // then
    expect(vi.mocked(useConstraints)).toHaveBeenCalledOnce();
    expect(queryByTestId('loading-spinner')).toBeInTheDocument();
  });

  it('should hide loading spinner when data fetch is complete', () => {
    // given
    vi.mocked(useConstraints).mockReturnValueOnce({
      ...useConstraintsStub,
      loading: computed(() => false),
    });

    // when
    const { queryByTestId } = render(ConstraintsView);

    // then
    expect(vi.mocked(useConstraints)).toHaveBeenCalledOnce();
    expect(queryByTestId('loading-spinner')).not.toBeInTheDocument();
  });

  it('should show error message when fetching data failed', () => {
    // given
    vi.mocked(useConstraints).mockReturnValueOnce({
      ...useConstraintsStub,
      loading: computed(() => false),
      error: ref(true),
    });

    // when
    const { queryByText } = render(ConstraintsView);

    // then
    expect(vi.mocked(useConstraints)).toHaveBeenCalledOnce();
    expect(
      queryByText('constraints.connectionError.title'),
    ).toBeInTheDocument();
    expect(queryByText('constraints.connectionError.text')).toBeInTheDocument();
  });

  it('should not show error message when data was fetch successfully', () => {
    // given
    vi.mocked(useConstraints).mockReturnValueOnce({
      ...useConstraintsStub,
      loading: computed(() => false),
      error: ref(false),
    });

    // when
    const { queryByText } = render(ConstraintsView);

    // then
    expect(vi.mocked(useConstraints)).toHaveBeenCalledOnce();
    expect(
      queryByText('constraints.connectionError.title'),
    ).not.toBeInTheDocument();
  });
});
