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
vi.mock(
  '@/views/admin/consistency/kafka-config-inconsistencies-listing/KafkaConfigInconsistenciesListing.vue',
  () => ({
    default: {
      template: '<div data-testid="kafka-config-listing" />',
    },
  }),
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

async function openOrphanTopicsSection(
  getByText: (text: string) => HTMLElement,
) {
  await fireEvent.click(getByText('consistency.inconsistentTopics.heading'));
}

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

  it('should keep large consistency sections collapsed at startup', () => {
    vi.mocked(useInconsistentTopics).mockReturnValueOnce(
      useInconsistentTopicsStub,
    );

    const { getByText, queryByTestId } = render(ConsistencyView);

    expect(getByText('consistency.kafkaConfig.heading')).toBeVisible();
    expect(queryByTestId('kafka-config-listing')).not.toBeInTheDocument();
    expect(getByText('consistency.inconsistentTopics.heading')).toBeVisible();
    expect(
      queryByTestId('selected-inconsistent-topics-count'),
    ).not.toBeInTheDocument();
  });

  it('loads orphan topics only when their section is expanded', async () => {
    const fetchInconsistentTopics = vi.fn(() => Promise.resolve());
    vi.mocked(useInconsistentTopics).mockReturnValueOnce({
      ...useInconsistentTopicsStub,
      fetchInconsistentTopics,
    });

    const { getByText } = render(ConsistencyView);
    expect(fetchInconsistentTopics).not.toHaveBeenCalled();

    await openOrphanTopicsSection(getByText);

    expect(fetchInconsistentTopics).toHaveBeenCalledOnce();
  });

  it('mounts Kafka configuration only when its section is expanded', async () => {
    vi.mocked(useInconsistentTopics).mockReturnValueOnce(
      useInconsistentTopicsStub,
    );

    const { getByText, getByTestId } = render(ConsistencyView);
    await fireEvent.click(getByText('consistency.kafkaConfig.heading'));

    expect(getByTestId('kafka-config-listing')).toBeVisible();
  });

  it('should show loading spinner when fetching Consistency data', async () => {
    // given
    vi.mocked(useInconsistentTopics).mockReturnValueOnce({
      ...useInconsistentTopicsStub,
      loading: ref(true),
    });

    // when
    const { getByText, queryByTestId } = render(ConsistencyView);
    await openOrphanTopicsSection(getByText);

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

  it('should show error message when fetching data failed', async () => {
    // given
    vi.mocked(useInconsistentTopics).mockReturnValueOnce({
      ...useInconsistentTopicsStub,
      loading: ref(false),
      error: ref({ fetchInconsistentTopics: new Error() }),
    });

    // when
    const { getByText, queryByText } = render(ConsistencyView);
    await openOrphanTopicsSection(getByText);

    // then
    expect(vi.mocked(useInconsistentTopics)).toHaveBeenCalledOnce();
    expect(queryByText('consistency.connectionError.title')).toBeVisible();
    expect(queryByText('consistency.connectionError.text')).toBeVisible();
  });

  it('should retry loading orphan topics after a failure', async () => {
    const fetchInconsistentTopics = vi.fn(() => Promise.resolve());
    vi.mocked(useInconsistentTopics).mockReturnValueOnce({
      ...useInconsistentTopicsStub,
      error: ref({ fetchInconsistentTopics: new Error() }),
      fetchInconsistentTopics,
    });

    const { getByText } = render(ConsistencyView);
    await openOrphanTopicsSection(getByText);
    fetchInconsistentTopics.mockClear();

    await fireEvent.click(
      getByText('consistency.inconsistentTopics.actions.retry'),
    );

    expect(fetchInconsistentTopics).toHaveBeenCalledOnce();
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
    await openOrphanTopicsSection(getByText);
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

  it('should finish deleting the original topic after another delete dialog is opened', async () => {
    // given
    let finishDeletion: (removed: boolean) => void = () => undefined;
    const removeInconsistentTopic = vi.fn(
      () =>
        new Promise<boolean>((resolve) => {
          finishDeletion = resolve;
        }),
    );
    const removeTopicsLocally = vi.fn();
    vi.mocked(useInconsistentTopics).mockReturnValueOnce({
      ...useInconsistentTopicsStub,
      removeInconsistentTopic,
      removeTopicsLocally,
    });
    const { getAllByText, getByText } = render(ConsistencyView, {
      testPinia: createTestingPiniaWithState(),
    });

    // when
    await openOrphanTopicsSection(getByText);
    await fireEvent.click(
      getAllByText('consistency.inconsistentTopics.actions.delete')[0],
    );
    await fireEvent.click(getByText('confirmationDialog.confirm'));
    await fireEvent.click(getByText('confirmationDialog.cancel'));
    await fireEvent.click(
      getAllByText('consistency.inconsistentTopics.actions.delete')[1],
    );
    finishDeletion(true);

    // then
    await waitFor(() => {
      expect(removeTopicsLocally).toHaveBeenCalledWith([
        dummyInconsistentTopics[0],
      ]);
      expect(
        getByText(
          'consistency.inconsistentTopics.confirmationDialog.remove.title',
        ),
      ).toBeInTheDocument();
    });
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
    await openOrphanTopicsSection(getByText);
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
    const topics = ref([...dummyInconsistentTopics]);
    const removeInconsistentTopic = vi
      .fn()
      .mockResolvedValueOnce(true)
      .mockResolvedValueOnce(false)
      .mockResolvedValueOnce(true);
    const removeTopicsLocally = vi.fn((removedTopics: string[]) => {
      topics.value = topics.value.filter(
        (topic) => !removedTopics.includes(topic),
      );
    });
    const fetchInconsistentTopics = vi.fn(async () => {
      topics.value = [...dummyInconsistentTopics];
    });
    vi.mocked(useInconsistentTopics).mockReturnValueOnce({
      ...useInconsistentTopicsStub,
      topics,
      fetchInconsistentTopics,
      removeInconsistentTopic,
      removeTopicsLocally,
    });
    const { getByLabelText, getByText, queryByText } = render(ConsistencyView, {
      testPinia: createTestingPiniaWithState(),
    });

    // when
    await openOrphanTopicsSection(getByText);
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
      expect(fetchInconsistentTopics).toHaveBeenCalledOnce();
      expect(queryByText(dummyInconsistentTopics[0])).not.toBeInTheDocument();
      expect(queryByText(dummyInconsistentTopics[2])).not.toBeInTheDocument();
      expect(getByLabelText(dummyInconsistentTopics[1])).toBeChecked();
    });
  });
});
