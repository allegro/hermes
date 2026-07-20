import {
  consistencyStoreState,
  createTestingPiniaWithState,
} from '@/dummy/store';
import { createTestingPinia } from '@pinia/testing';
import { dummyInconsistentTopics } from '@/dummy/inconsistentTopics';
import { expect } from 'vitest';
import { fireEvent, waitFor } from '@testing-library/vue';
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
  fetchInconsistentTopics: () => Promise.resolve(),
  removeTopicsLocally: () => undefined,
  removeInconsistentTopic: () => Promise.resolve(true),
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

  it('should show progress bar when fetching consistency data', () => {
    // when
    vi.mocked(useInconsistentTopics).mockReturnValueOnce(
      useInconsistentTopicsStub,
    );
    const { queryByTestId } = render(ConsistencyView, {
      testPinia: createTestingPinia({
        initialState: {
          consistency: {
            ...consistencyStoreState,
            fetchInProgress: true,
          },
        },
        stubActions: false,
      }),
    });

    // then
    expect(queryByTestId('consistency-progress-bar')).toBeVisible();
  });

  it('should not show progress bar when fetching consistency data is not in progress', () => {
    // when
    vi.mocked(useInconsistentTopics).mockReturnValueOnce(
      useInconsistentTopicsStub,
    );
    const { queryByTestId } = render(ConsistencyView, {
      testPinia: createTestingPinia({
        initialState: {
          consistency: {
            ...consistencyStoreState,
            fetchInProgress: false,
          },
        },
        stubActions: false,
      }),
    });

    // then
    expect(queryByTestId('consistency-progress-bar')).not.toBeInTheDocument();
  });

  it('should show error message when fetching consistency failed', () => {
    // given
    vi.mocked(useInconsistentTopics).mockReturnValueOnce(
      useInconsistentTopicsStub,
    );

    // when
    const { queryByText } = render(ConsistencyView, {
      testPinia: createTestingPinia({
        initialState: {
          consistency: {
            ...consistencyStoreState,
            error: {
              fetchError: true,
            },
          },
        },
        stubActions: false,
      }),
    });

    // then
    expect(queryByText('consistency.connectionError.title')).toBeVisible();
    expect(queryByText('consistency.connectionError.text')).toBeVisible();
  });

  it('should not show error message when fetching consistency succeeded', () => {
    // given
    vi.mocked(useInconsistentTopics).mockReturnValueOnce(
      useInconsistentTopicsStub,
    );

    // when
    const { queryByText } = render(ConsistencyView, {
      testPinia: createTestingPinia({
        initialState: {
          consistency: {
            ...consistencyStoreState,
            error: {
              fetchError: null,
            },
          },
        },
        stubActions: false,
      }),
    });

    // then
    expect(
      queryByText('consistency.connectionError.title'),
    ).not.toBeInTheDocument();
    expect(
      queryByText('consistency.connectionError.text'),
    ).not.toBeInTheDocument();
  });

  it('should show confirmation dialog on remove button click', async () => {
    // given
    vi.mocked(useInconsistentTopics).mockReturnValueOnce(
      useInconsistentTopicsStub,
    );

    // when
    const { getAllByText, getByText } = render(ConsistencyView, {
      testPinia: createTestingPiniaWithState(),
    });
    await fireEvent.click(
      getAllByText('consistency.inconsistentTopics.actions.delete')[0],
    );

    // then
    expect(
      getByText(
        'consistency.inconsistentTopics.confirmationDialog.remove.title',
      ),
    ).toBeInTheDocument();
    expect(
      getByText(
        'consistency.inconsistentTopics.confirmationDialog.remove.text',
      ),
    ).toBeInTheDocument();
  });

  it('should show one confirmation dialog for selected inconsistent topics', async () => {
    // given
    vi.mocked(useInconsistentTopics).mockReturnValueOnce(
      useInconsistentTopicsStub,
    );
    const { getByLabelText, getByText } = render(ConsistencyView, {
      testPinia: createTestingPiniaWithState(),
    });

    // when
    await fireEvent.click(getByLabelText(dummyInconsistentTopics[0]));
    await fireEvent.click(
      getByText('consistency.inconsistentTopics.actions.removeSelected'),
    );

    // then
    expect(
      getByText('consistency.inconsistentTopics.batch.confirmation.title'),
    ).toBeInTheDocument();
    expect(
      getByText('consistency.inconsistentTopics.batch.confirmation.text'),
    ).toBeInTheDocument();
  });

  it('should continue batch removal after a topic deletion fails', async () => {
    // given
    const removeInconsistentTopic = vi
      .fn()
      .mockResolvedValueOnce(true)
      .mockResolvedValueOnce(false)
      .mockResolvedValueOnce(true);
    const removeTopicsLocally = vi.fn();
    vi.mocked(useInconsistentTopics).mockReturnValueOnce({
      ...useInconsistentTopicsStub,
      removeInconsistentTopic,
      removeTopicsLocally,
    });
    const { getByLabelText, getByText } = render(ConsistencyView, {
      testPinia: createTestingPiniaWithState(),
    });

    // when
    for (const topic of dummyInconsistentTopics) {
      await fireEvent.click(getByLabelText(topic));
    }
    await fireEvent.click(
      getByText('consistency.inconsistentTopics.actions.removeSelected'),
    );
    await fireEvent.click(getByText('confirmationDialog.confirm'));

    // then
    await waitFor(() => {
      expect(removeInconsistentTopic).toHaveBeenCalledTimes(3);
      expect(removeTopicsLocally).toHaveBeenCalledWith([
        dummyInconsistentTopics[0],
        dummyInconsistentTopics[2],
      ]);
    });
  });
});
