import { dummyConstraints } from '@/dummy/constraints';
import { expect } from 'vitest';
import { ref } from 'vue';
import { render } from '@/utils/test-utils';
import { useConstraints } from '@/composables/constraints/use-constraints/useConstraints';
import ConstraintsView from '@/views/admin/constraints/ConstraintsView.vue';
import userEvent from '@testing-library/user-event';
import type { UseConstraints } from '@/composables/constraints/use-constraints/useConstraints';

vi.mock('@/composables/constraints/use-constraints/useConstraints');

const useConstraintsStub: UseConstraints = {
  topicConstraints: ref(dummyConstraints.topicConstraints),
  subscriptionConstraints: ref(dummyConstraints.subscriptionConstraints),
  upsertTopicConstraint: () => Promise.resolve(),
  upsertSubscriptionConstraint: () => Promise.resolve(),
  deleteTopicConstraint: () => Promise.resolve(),
  deleteSubscriptionConstraint: () => Promise.resolve(),
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

  it('should open create subscription constraint dialog when add subscription constraint is clicked', async () => {
    // given
    vi.mocked(useConstraints).mockReturnValueOnce(useConstraintsStub);
    const user = userEvent.setup();

    // when
    const { getByTestId, queryByText } = render(ConstraintsView);

    // then
    await user.click(getByTestId('addSubscriptionConstraint'));
    expect(
      queryByText('constraints.createForm.createSubscriptionTitle'),
    ).toBeVisible();
  });

  it('should open create topic constraint dialog when add topic constraint is clicked', async () => {
    // given
    vi.mocked(useConstraints).mockReturnValueOnce(useConstraintsStub);
    const user = userEvent.setup();

    // when
    const { getByTestId, queryByText } = render(ConstraintsView);

    // then
    await user.click(getByTestId('addTopicConstraint'));
    expect(
      queryByText('constraints.createForm.createTopicTitle'),
    ).toBeVisible();
  });
});
