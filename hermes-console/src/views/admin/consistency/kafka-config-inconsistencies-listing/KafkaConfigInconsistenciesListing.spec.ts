import { createTestingPinia } from '@pinia/testing';
import { dummyKafkaConfigInconsistencies } from '@/dummy/kafkaConfigInconsistencies';
import { fireEvent, within } from '@testing-library/vue';
import { ref } from 'vue';
import { render } from '@/utils/test-utils';
import { useKafkaConfigConsistency } from '@/composables/kafka-config-consistency/use-kafka-config-consistency/useKafkaConfigConsistency';
import KafkaConfigInconsistenciesListing from '@/views/admin/consistency/kafka-config-inconsistencies-listing/KafkaConfigInconsistenciesListing.vue';
import type { UseKafkaConfigConsistency } from '@/composables/kafka-config-consistency/use-kafka-config-consistency/useKafkaConfigConsistency';

vi.mock(
  '@/composables/kafka-config-consistency/use-kafka-config-consistency/useKafkaConfigConsistency',
);

const reviewSync = vi.fn(() => Promise.resolve(true));
const composableStub: UseKafkaConfigConsistency = {
  inconsistencies: ref(dummyKafkaConfigInconsistencies),
  loading: ref(false),
  error: ref({ fetch: null, action: null }),
  batchProgress: ref(undefined),
  batchTotal: ref(0),
  batchResult: ref(null),
  fetchClusters: () => Promise.resolve(),
  fetchInconsistencies: () => Promise.resolve(),
  syncTopic: () => Promise.resolve(null),
  reviewSync,
  applyReviewedSync: () => Promise.resolve({ successful: 0, failed: 0 }),
  reviewBootstrap: () => Promise.resolve(true),
  applyReviewedBootstrap: () => Promise.resolve(true),
  removeTopicsLocally: () => undefined,
};

describe('KafkaConfigInconsistenciesListing', () => {
  beforeEach(() => {
    reviewSync.mockClear();
    vi.mocked(useKafkaConfigConsistency).mockReturnValue(composableStub);
  });

  it('renders topics and expands per-key configuration diffs', async () => {
    const { getByText, getAllByText } = render(
      KafkaConfigInconsistenciesListing,
      {
        testPinia: createTestingPinia({
          initialState: {
            kafkaConfigConsistency: {
              clusters: ['gcp'],
              selectedCluster: 'gcp',
              lastDryRun: null,
            },
          },
        }),
      },
    );

    expect(
      getByText(dummyKafkaConfigInconsistencies[0].qualifiedTopicName),
    ).toBeVisible();
    await fireEvent.click(
      getAllByText('consistency.kafkaConfig.actions.diffs')[0],
    );

    const diffRow = getByText('retention.ms').closest('tr')!;
    expect(within(diffRow).getByText('86400000')).toBeVisible();
    expect(
      within(diffRow).getByText('consistency.kafkaConfig.diffs.unset'),
    ).toBeVisible();
  });

  it('reviews selected present topics in dry-run mode', async () => {
    const { getByLabelText, getByText } = render(
      KafkaConfigInconsistenciesListing,
      {
        testPinia: createTestingPinia({
          initialState: {
            kafkaConfigConsistency: {
              clusters: ['gcp'],
              selectedCluster: 'gcp',
              lastDryRun: null,
            },
          },
        }),
      },
    );
    const topic = dummyKafkaConfigInconsistencies[0];

    await fireEvent.click(
      getByLabelText(`${topic.clusterName}:${topic.kafkaTopicName}`),
    );
    await fireEvent.click(
      getByText('consistency.kafkaConfig.actions.syncSelected'),
    );

    expect(reviewSync).toHaveBeenCalledWith([topic]);
  });
});
