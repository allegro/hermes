import { dummyInactiveTopics } from '@/dummy/inactiveTopics';
import { expect } from 'vitest';
import { ref } from 'vue';
import { render } from '@/utils/test-utils';
import { useInactiveTopics } from '@/composables/inactive-topics/use-inactive-topics/useInactiveTopics';
import InactiveTopicsView from '@/views/admin/inactive-topics/InactiveTopicsView.vue';
import type { UseInactiveTopics } from '@/composables/inactive-topics/use-inactive-topics/useInactiveTopics';

vi.mock('@/composables/inactive-topics/use-inactive-topics/useInactiveTopics');

const useInactiveTopicsStub: UseInactiveTopics = {
  inactiveTopics: ref(dummyInactiveTopics),
  loading: ref(false),
  error: ref({ fetchInactiveTopics: null }),
};

describe('InactiveTopicsView', () => {
  it('should render if inactive topics data was successfully fetched', () => {
    // given
    vi.mocked(useInactiveTopics).mockReturnValueOnce(useInactiveTopicsStub);

    // when
    const { getByText } = render(InactiveTopicsView);

    // then
    expect(vi.mocked(useInactiveTopics)).toHaveBeenCalledOnce();
    expect(getByText('inactiveTopics.heading')).toBeVisible();
  });

  it('should show loading spinner when fetching inactive topics data', () => {
    // given
    vi.mocked(useInactiveTopics).mockReturnValueOnce({
      ...useInactiveTopicsStub,
      loading: ref(true),
    });

    // when
    const { queryByTestId } = render(InactiveTopicsView);

    // then
    expect(vi.mocked(useInactiveTopics)).toHaveBeenCalledOnce();
    expect(queryByTestId('loading-spinner')).toBeVisible();
  });

  it('should hide loading spinner when data fetch is complete', () => {
    // given
    vi.mocked(useInactiveTopics).mockReturnValueOnce({
      ...useInactiveTopicsStub,
      loading: ref(false),
    });

    // when
    const { queryByTestId } = render(InactiveTopicsView);

    // then
    expect(vi.mocked(useInactiveTopics)).toHaveBeenCalledOnce();
    expect(queryByTestId('loading-spinner')).not.toBeInTheDocument();
  });

  it('should show error message when fetching data failed', () => {
    // given
    vi.mocked(useInactiveTopics).mockReturnValueOnce({
      ...useInactiveTopicsStub,
      loading: ref(false),
      error: ref({ fetchInactiveTopics: new Error() }),
    });

    // when
    const { queryByText } = render(InactiveTopicsView);

    // then
    expect(vi.mocked(useInactiveTopics)).toHaveBeenCalledOnce();
    expect(queryByText('inactiveTopics.connectionError.title')).toBeVisible();
    expect(queryByText('inactiveTopics.connectionError.text')).toBeVisible();
  });

  it('should not show error message when data was fetch successfully', () => {
    // given
    vi.mocked(useInactiveTopics).mockReturnValueOnce({
      ...useInactiveTopicsStub,
      loading: ref(false),
      error: ref({ fetchInactiveTopics: null }),
    });

    // when
    const { queryByText } = render(InactiveTopicsView);

    // then
    expect(vi.mocked(useInactiveTopics)).toHaveBeenCalledOnce();
    expect(
      queryByText('inactiveTopics.connectionError.title'),
    ).not.toBeInTheDocument();
  });
});
