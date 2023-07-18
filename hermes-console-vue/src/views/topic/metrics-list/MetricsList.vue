<script setup lang="ts">
  import { formatNumber } from '@/utils/number-formatter/number-formatter';
  import KeyValueCard from '@/components/key-value-card/KeyValueCard.vue';
  import KeyValueCardItem from '@/components/key-value-card/key-value-card-item/KeyValueCardItem.vue';
  import type { TopicMetrics } from '@/api/topic';

  const props = defineProps<{
    metrics: TopicMetrics;
  }>();
</script>

<template>
  <key-value-card :title="$t('topicView.metrics.title')">
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
