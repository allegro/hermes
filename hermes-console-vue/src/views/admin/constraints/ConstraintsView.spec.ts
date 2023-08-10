import { dummyConstraints } from '@/dummy/constraints';
import { ref } from 'vue';
import { render } from '@/utils/test-utils';
import { useConstraints } from '@/composables/constraints/use-constraints/useConstraints';
import ConstraintsView from '@/views/admin/constraints/ConstraintsView.vue';
import type { UseConstraints } from '@/composables/constraints/use-constraints/useConstraints';

vi.mock('@/composables/constraints/use-constraints/useConstraints');

const useConstraintsStub: UseConstraints = {
  topicConstraints: ref(dummyConstraints.topicConstraints),
  subscriptionConstraints: ref(dummyConstraints.subscriptionConstraints),
  loading: ref(false),
  error: ref({
    fetchConstraints: null,
  }),
};

describe('ConstraintsView', () => {
  it('should render if constraints data was successfully fetched', () => {
    // given
    vi.mocked(useConstraints).mockReturnValueOnce(useConstraintsStub);

    // when
    const { getByText } = render(ConstraintsView);

    // then
    expect(vi.mocked(useConstraints)).toHaveBeenCalledOnce();
    expect(getByText('constraints.topicConstraints.heading')).toBeVisible();
    expect(
      getByText('constraints.subscriptionConstraints.heading'),
    ).toBeVisible();
  });

  it('should show loading spinner when fetching Constraints data', () => {
    // given
    vi.mocked(useConstraints).mockReturnValueOnce({
      ...useConstraintsStub,
      loading: ref(true),
    });

    // when
    const { queryByTestId } = render(ConstraintsView);

    // then
    expect(vi.mocked(useConstraints)).toHaveBeenCalledOnce();
    expect(queryByTestId('loading-spinner')).toBeVisible();
  });

  it('should hide loading spinner when data fetch is complete', () => {
    // given
    vi.mocked(useConstraints).mockReturnValueOnce({
      ...useConstraintsStub,
      loading: ref(false),
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
      loading: ref(false),
      error: ref({ fetchConstraints: new Error() }),
    });

    // when
    const { queryByText } = render(ConstraintsView);

    // then
    expect(vi.mocked(useConstraints)).toHaveBeenCalledOnce();
    expect(queryByText('constraints.connectionError.title')).toBeVisible();
    expect(queryByText('constraints.connectionError.text')).toBeVisible();
  });

  it('should not show error message when data was fetch successfully', () => {
    // given
    vi.mocked(useConstraints).mockReturnValueOnce({
      ...useConstraintsStub,
      loading: ref(false),
      error: ref({ fetchConstraints: null }),
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
