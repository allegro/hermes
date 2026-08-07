import { defineStore } from 'pinia';
import type { InconsistentKafkaTopic } from '@/api/kafka-inconsistency';

export type KafkaConfigDryRun =
  | {
      operation: 'sync';
      clusterName: string | null;
      inconsistencies: InconsistentKafkaTopic[];
    }
  | {
      operation: 'bootstrap';
      clusterName: string;
      topicNames: string[];
    };

interface KafkaConfigConsistencyState {
  clusters: string[];
  selectedCluster: string | null;
  lastDryRun: KafkaConfigDryRun | null;
}

export const useKafkaConfigConsistencyStore = defineStore(
  'kafkaConfigConsistency',
  {
    state: (): KafkaConfigConsistencyState => ({
      clusters: [],
      selectedCluster: null,
      lastDryRun: null,
    }),
    actions: {
      setClusters(clusters: string[]) {
        this.clusters = clusters;
        if (
          this.selectedCluster !== null &&
          !clusters.includes(this.selectedCluster)
        ) {
          this.selectedCluster = null;
          this.lastDryRun = null;
        }
      },
      selectCluster(clusterName: string | null) {
        if (this.selectedCluster !== clusterName) {
          this.lastDryRun = null;
        }
        this.selectedCluster = clusterName;
      },
      saveDryRun(dryRun: KafkaConfigDryRun) {
        this.lastDryRun = dryRun;
      },
      clearDryRun() {
        this.lastDryRun = null;
      },
    },
    persist: true,
  },
);
