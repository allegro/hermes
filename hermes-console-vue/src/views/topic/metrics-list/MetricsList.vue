<script setup lang="ts">
  import { formatNumber } from '@/utils/number-formatter/number-formatter';
  import { useAppConfigStore } from '@/store/app-config/useAppConfigStore';
  import { useMetrics } from '@/composables/metrics/use-metrics/useMetrics';
  import KeyValueCard from '@/components/key-value-card/KeyValueCard.vue';
  import KeyValueCardItem from '@/components/key-value-card/key-value-card-item/KeyValueCardItem.vue';
  import type { TopicMetrics } from '@/api/topic';

  const props = defineProps<{
    topicName: string;
    metrics: TopicMetrics;
  }>();

  const { loadedConfig } = useAppConfigStore();

  const dashboardUrl = loadedConfig.metrics.fetchingDashboardUrlEnabled
    ? useMetrics(props.topicName, null).dashboardUrl
    : null;
</script>

<template>
  <key-value-card
    :title="$t('topicView.metrics.title')"
    :button-text="$t('topicView.metrics.dashboard')"
    :button-href="dashboardUrl"
  >
    <key-value-card-item
      :name="$t('topicView.metrics.rate')"
      :value="formatNumber(props.metrics.rate, 2)"
    />
    <key-value-card-item
      :name="$t('topicView.metrics.deliveryRate')"
      :value="formatNumber(props.metrics.deliveryRate, 2)"
    />
    <key-value-card-item
      :name="$t('topicView.metrics.published')"
      :value="formatNumber(props.metrics.published)"
    />
    <key-value-card-item :name="$t('topicView.metrics.latency')" value="?" />
    <key-value-card-item
      :name="$t('topicView.metrics.messageSize')"
      value="?"
    />
  </key-value-card>
</template>

<style scoped lang="scss"></style>
