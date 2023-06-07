<script setup lang="ts">
  import { useI18n } from 'vue-i18n';
  import KeyValueCard from '@/components/key-value-card/KeyValueCard.vue';
  import KeyValueCardItem from '@/components/key-value-card/key-value-card-item/KeyValueCardItem.vue';
  import type { TopicMetrics } from '@/api/topic';

  const props = defineProps<{
    metrics: TopicMetrics;
  }>();

  const { t } = useI18n();
</script>

<template>
  <key-value-card :title="t('topicView.metrics.title')">
    <key-value-card-item
      :name="t('topicView.metrics.rate')"
      :value="props.metrics.rate"
    />
    <key-value-card-item
      :name="t('topicView.metrics.deliveryRate')"
      :value="props.metrics.deliveryRate"
    />
    <key-value-card-item
      :name="t('topicView.metrics.published')"
      :value="props.metrics.published"
    />
    <key-value-card-item :name="t('topicView.metrics.latency')" value="?" />
    <key-value-card-item :name="t('topicView.metrics.messageSize')" value="?" />
  </key-value-card>
</template>

<style scoped lang="scss"></style>
