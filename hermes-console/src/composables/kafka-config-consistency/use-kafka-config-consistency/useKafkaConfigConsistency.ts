import {
  bootstrapCluster,
  fetchKafkaClusters,
  fetchKafkaConfigInconsistencies,
  syncAllKafkaTopicConfigs,
  syncKafkaTopicConfig,
} from '@/api/hermes-client';
import { computed, ref } from 'vue';
import { dispatchErrorNotification } from '@/utils/notification-utils';
import { topicKey } from '@/api/kafka-inconsistency';
import { useGlobalI18n } from '@/i18n';
import { useKafkaConfigConsistencyStore } from '@/store/consistency/useKafkaConfigConsistencyStore';
import { useNotificationsStore } from '@/store/app-notifications/useAppNotifications';
import type { InconsistentKafkaTopic } from '@/api/kafka-inconsistency';
import type { Ref } from 'vue';

export interface KafkaConfigBatchResult {
  successful: number;
  failed: number;
}

export interface UseKafkaConfigConsistencyErrors {
  fetch: Error | null;
  action: Error | null;
}

export interface UseKafkaConfigConsistency {
  inconsistencies: Ref<InconsistentKafkaTopic[]>;
  loading: Ref<boolean>;
  error: Ref<UseKafkaConfigConsistencyErrors>;
  batchProgress: Ref<number | undefined>;
  batchTotal: Ref<number>;
  batchResult: Ref<KafkaConfigBatchResult | null>;
  fetchClusters: () => Promise<void>;
  fetchInconsistencies: (clusterName?: string) => Promise<void>;
  syncTopic: (
    topic: InconsistentKafkaTopic,
    dryRun?: boolean,
    notify?: boolean,
  ) => Promise<InconsistentKafkaTopic | null>;
  reviewSync: (topics?: InconsistentKafkaTopic[]) => Promise<boolean>;
  applyReviewedSync: () => Promise<KafkaConfigBatchResult>;
  reviewBootstrap: (clusterName: string) => Promise<boolean>;
  applyReviewedBootstrap: () => Promise<boolean>;
  removeTopicsLocally: (topics: InconsistentKafkaTopic[]) => void;
}

const concurrency = 5;

export function useKafkaConfigConsistency(): UseKafkaConfigConsistency {
  const consistencyStore = useKafkaConfigConsistencyStore();
  const notificationStore = useNotificationsStore();
  const rawInconsistencies = ref<InconsistentKafkaTopic[]>([]);
  const loading = ref(false);
  const error = ref<UseKafkaConfigConsistencyErrors>({
    fetch: null,
    action: null,
  });
  const batchProgress = ref<number>();
  const batchTotal = ref(0);
  const batchResult = ref<KafkaConfigBatchResult | null>(null);
  let latestFetchId = 0;

  // The backend already returns rows ordered by cluster, topic and kafka topic name.
  const inconsistencies = computed(() => rawInconsistencies.value);

  async function fetchClusters() {
    try {
      error.value.fetch = null;
      consistencyStore.setClusters((await fetchKafkaClusters()).data);
    } catch (e) {
      error.value.fetch = e as Error;
    }
  }

  async function fetchInconsistencies(clusterName?: string) {
    const fetchId = ++latestFetchId;
    try {
      loading.value = true;
      error.value.fetch = null;
      const result = await fetchKafkaConfigInconsistencies(clusterName);
      if (fetchId === latestFetchId) rawInconsistencies.value = result.data;
    } catch (e) {
      if (fetchId === latestFetchId) error.value.fetch = e as Error;
    } finally {
      if (fetchId === latestFetchId) loading.value = false;
    }
  }

  function removeTopicsLocally(topics: InconsistentKafkaTopic[]) {
    const keys = new Set(topics.map(topicKey));
    rawInconsistencies.value = rawInconsistencies.value.filter(
      (topic) => !keys.has(topicKey(topic)),
    );
  }

  async function syncTopic(
    topic: InconsistentKafkaTopic,
    dryRun = true,
    notify = true,
  ): Promise<InconsistentKafkaTopic | null> {
    try {
      if (!topic.existsOnBroker) return null;
      const response = await syncKafkaTopicConfig(
        topic.qualifiedTopicName,
        topic.kafkaTopicName,
        topic.clusterName,
        dryRun,
      );
      if (notify) {
        await notificationStore.dispatchNotification({
          text: useGlobalI18n().t(
            `notifications.kafkaTopicConfig.sync.${dryRun ? 'preview' : 'success'}`,
            { topic: topic.qualifiedTopicName },
          ),
          type: 'success',
        });
      }
      return response.data || topic;
    } catch (e: any) {
      error.value.action = e as Error;
      if (notify) {
        dispatchErrorNotification(
          e,
          notificationStore,
          useGlobalI18n().t('notifications.kafkaTopicConfig.sync.failure', {
            topic: topic.qualifiedTopicName,
          }),
        );
      }
      return null;
    }
  }

  async function reviewSync(topics?: InconsistentKafkaTopic[]) {
    error.value.action = null;
    batchResult.value = null;
    consistencyStore.clearDryRun();
    try {
      let reviewed: InconsistentKafkaTopic[];
      if (topics === undefined) {
        reviewed = (
          await syncAllKafkaTopicConfigs(
            consistencyStore.selectedCluster || undefined,
            true,
          )
        ).data;
      } else {
        if (topics.some((topic) => !topic.existsOnBroker)) return false;
        const results = await Promise.all(
          topics.map((topic) => syncTopic(topic, true, false)),
        );
        if (results.some((result) => result === null)) return false;
        reviewed = results.filter(
          (result): result is InconsistentKafkaTopic => result !== null,
        );
      }
      consistencyStore.saveDryRun({
        operation: 'sync',
        clusterName: consistencyStore.selectedCluster,
        inconsistencies: reviewed,
      });
      return true;
    } catch (e: any) {
      error.value.action = e as Error;
      dispatchErrorNotification(
        e,
        notificationStore,
        useGlobalI18n().t('notifications.kafkaTopicConfig.sync.failure'),
      );
      return false;
    }
  }

  async function applyReviewedSync(): Promise<KafkaConfigBatchResult> {
    const dryRun = consistencyStore.lastDryRun;
    if (!dryRun || dryRun.operation !== 'sync') {
      return { successful: 0, failed: 0 };
    }

    const uniqueTopics = [
      ...new Map(
        dryRun.inconsistencies.map((topic) => [topicKey(topic), topic]),
      ).values(),
    ];
    const result = { successful: 0, failed: 0 };
    let nextTopicIndex = 0;
    batchProgress.value = 0;
    batchTotal.value = uniqueTopics.length;

    async function syncNextTopic() {
      while (nextTopicIndex < uniqueTopics.length) {
        const topic = uniqueTopics[nextTopicIndex++];
        if (await syncTopic(topic, false, false)) {
          result.successful++;
        } else {
          result.failed++;
        }
        batchProgress.value!++;
      }
    }

    await Promise.all(
      Array.from({ length: Math.min(concurrency, uniqueTopics.length) }, () =>
        syncNextTopic(),
      ),
    );
    batchResult.value = result;
    batchProgress.value = undefined;
    batchTotal.value = 0;
    consistencyStore.clearDryRun();
    await fetchInconsistencies(consistencyStore.selectedCluster || undefined);
    await notificationStore.dispatchNotification({
      text: useGlobalI18n().t(
        'notifications.kafkaTopicConfig.sync.complete',
        result,
      ),
      type: result.failed === 0 ? 'success' : 'warning',
    });
    return result;
  }

  async function reviewBootstrap(clusterName: string) {
    try {
      error.value.action = null;
      consistencyStore.clearDryRun();
      const topicNames = (await bootstrapCluster(clusterName, true)).data;
      consistencyStore.saveDryRun({
        operation: 'bootstrap',
        clusterName,
        topicNames,
      });
      return true;
    } catch (e: any) {
      error.value.action = e as Error;
      dispatchErrorNotification(
        e,
        notificationStore,
        useGlobalI18n().t('notifications.kafkaTopicConfig.bootstrap.failure'),
      );
      return false;
    }
  }

  async function applyReviewedBootstrap() {
    const dryRun = consistencyStore.lastDryRun;
    if (!dryRun || dryRun.operation !== 'bootstrap') return false;
    try {
      await bootstrapCluster(dryRun.clusterName, false);
      consistencyStore.clearDryRun();
      await fetchInconsistencies(dryRun.clusterName);
      await notificationStore.dispatchNotification({
        text: useGlobalI18n().t(
          'notifications.kafkaTopicConfig.bootstrap.success',
          { count: dryRun.topicNames.length },
        ),
        type: 'success',
      });
      return true;
    } catch (e: any) {
      error.value.action = e as Error;
      dispatchErrorNotification(
        e,
        notificationStore,
        useGlobalI18n().t('notifications.kafkaTopicConfig.bootstrap.failure'),
      );
      return false;
    }
  }

  async function initialize() {
    await fetchClusters();
    if (!error.value.fetch) {
      await fetchInconsistencies(consistencyStore.selectedCluster || undefined);
    }
  }

  void initialize();

  return {
    inconsistencies,
    loading,
    error,
    batchProgress,
    batchTotal,
    batchResult,
    fetchClusters,
    fetchInconsistencies,
    syncTopic,
    reviewSync,
    applyReviewedSync,
    reviewBootstrap,
    applyReviewedBootstrap,
    removeTopicsLocally,
  };
}
