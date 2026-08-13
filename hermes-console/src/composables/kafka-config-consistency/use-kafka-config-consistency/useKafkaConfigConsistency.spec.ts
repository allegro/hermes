import { afterEach, beforeEach, describe, expect } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';
import {
  dummyKafkaClusters,
  dummyKafkaConfigInconsistencies,
} from '@/dummy/kafkaConfigInconsistencies';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { useKafkaConfigConsistency } from '@/composables/kafka-config-consistency/use-kafka-config-consistency/useKafkaConfigConsistency';
import { useKafkaConfigConsistencyStore } from '@/store/consistency/useKafkaConfigConsistencyStore';
import { waitFor } from '@testing-library/vue';

const url = 'http://localhost:3000';
const server = setupServer();

describe('useKafkaConfigConsistency', () => {
  beforeAll(() => server.listen());

  beforeEach(() => {
    setActivePinia(createPinia());
    server.use(
      http.get(`${url}/consistency/kafka/clusters`, () =>
        HttpResponse.json(dummyKafkaClusters),
      ),
      http.get(`${url}/consistency/kafka/topics/config/inconsistencies`, () =>
        HttpResponse.json(dummyKafkaConfigInconsistencies),
      ),
    );
  });

  afterEach(() => server.resetHandlers());
  afterAll(() => server.close());

  it('fetches clusters and inconsistencies during initialization', async () => {
    const store = useKafkaConfigConsistencyStore();

    const { inconsistencies, loading, error } = useKafkaConfigConsistency();

    await waitFor(() => {
      expect(loading.value).toBe(false);
      expect(store.clusters).toEqual(dummyKafkaClusters);
      expect(inconsistencies.value).toEqual(dummyKafkaConfigInconsistencies);
      expect(error.value.fetch).toBeNull();
    });
  });

  it('dry-runs selected topics before applying them', async () => {
    const dryRunValues: string[] = [];
    const kafkaTopicNames: string[] = [];
    server.use(
      http.post(
        `${url}/consistency/kafka/topics/:topic/config/sync`,
        ({ request }) => {
          dryRunValues.push(new URL(request.url).searchParams.get('dryRun')!);
          kafkaTopicNames.push(
            new URL(request.url).searchParams.get('kafkaTopicName')!,
          );
          return HttpResponse.json(dummyKafkaConfigInconsistencies[0]);
        },
      ),
    );
    const store = useKafkaConfigConsistencyStore();
    store.selectCluster('gcp');
    const consistency = useKafkaConfigConsistency();
    await waitFor(() => expect(consistency.loading.value).toBe(false));

    expect(
      await consistency.reviewSync([dummyKafkaConfigInconsistencies[0]]),
    ).toBe(true);
    expect(store.lastDryRun?.operation).toBe('sync');

    await consistency.applyReviewedSync();

    expect(dryRunValues).toEqual(['true', 'false']);
    expect(kafkaTopicNames).toEqual([
      dummyKafkaConfigInconsistencies[0].kafkaTopicName,
      dummyKafkaConfigInconsistencies[0].kafkaTopicName,
    ]);
    expect(store.lastDryRun).toBeNull();
    expect(consistency.batchResult.value).toEqual({ successful: 1, failed: 0 });
  });

  it('dry-runs bootstrap before applying it', async () => {
    const dryRunValues: string[] = [];
    server.use(
      http.post(
        `${url}/consistency/kafka/clusters/:cluster/bootstrap`,
        ({ request }) => {
          dryRunValues.push(new URL(request.url).searchParams.get('dryRun')!);
          return HttpResponse.json(['pl.allegro.public.order.OrderEventV1']);
        },
      ),
    );
    const store = useKafkaConfigConsistencyStore();
    const consistency = useKafkaConfigConsistency();
    await waitFor(() => expect(consistency.loading.value).toBe(false));

    expect(await consistency.reviewBootstrap('gcp')).toBe(true);
    expect(store.lastDryRun).toEqual({
      operation: 'bootstrap',
      clusterName: 'gcp',
      topicNames: ['pl.allegro.public.order.OrderEventV1'],
    });

    expect(await consistency.applyReviewedBootstrap()).toBe(true);
    expect(dryRunValues).toEqual(['true', 'false']);
  });
});
