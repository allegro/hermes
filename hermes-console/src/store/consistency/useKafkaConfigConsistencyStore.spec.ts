import { beforeEach, describe, expect } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';
import { dummyKafkaConfigInconsistencies } from '@/dummy/kafkaConfigInconsistencies';
import { useKafkaConfigConsistencyStore } from '@/store/consistency/useKafkaConfigConsistencyStore';

describe('useKafkaConfigConsistencyStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
  });

  it('keeps clusters, selected cluster and dry-run result', () => {
    const store = useKafkaConfigConsistencyStore();

    store.setClusters(['dc', 'gcp']);
    store.selectCluster('gcp');
    store.saveDryRun({
      operation: 'sync',
      clusterName: 'gcp',
      inconsistencies: dummyKafkaConfigInconsistencies,
    });

    expect(store.clusters).toEqual(['dc', 'gcp']);
    expect(store.selectedCluster).toBe('gcp');
    expect(store.lastDryRun).toEqual({
      operation: 'sync',
      clusterName: 'gcp',
      inconsistencies: dummyKafkaConfigInconsistencies,
    });
  });

  it('clears a selected cluster that is no longer available', () => {
    const store = useKafkaConfigConsistencyStore();
    store.setClusters(['gcp']);
    store.selectCluster('gcp');
    store.saveDryRun({
      operation: 'bootstrap',
      clusterName: 'gcp',
      topicNames: ['pl.allegro.public.order.OrderEventV1'],
    });

    store.setClusters(['dc']);

    expect(store.selectedCluster).toBeNull();
    expect(store.lastDryRun).toBeNull();
  });

  it('clears a dry-run result when cluster scope changes', () => {
    const store = useKafkaConfigConsistencyStore();
    store.selectCluster('gcp');
    store.saveDryRun({
      operation: 'sync',
      clusterName: 'gcp',
      inconsistencies: dummyKafkaConfigInconsistencies,
    });

    store.selectCluster('dc');

    expect(store.lastDryRun).toBeNull();
  });
});
