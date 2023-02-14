<script setup lang="ts">
  import { formatNumber } from '@/utils/number-formatter/number-formatter';
  import KeyValueCard from '@/components/key-value-card/KeyValueCard.vue';
  import type { SubscriptionMetrics } from '@/api/subscription-metrics';

  interface MetricsCardProps {
    metrics: SubscriptionMetrics;
  }

  const props = defineProps<MetricsCardProps>();

  const entries = [
    {
      name: 'Delivery rate',
      value: formatNumber(props.metrics.rate, 2),
    },
    {
      name: 'Subscriber latency',
      tooltip:
        'Latency of acknowledging messages by subscribing service as ' +
        'measured by Hermes.',
    },
    {
      name: 'Delivered',
      value: formatNumber(props.metrics.delivered),
    },
    {
      name: 'Discarded',
      value: formatNumber(props.metrics.discarded),
    },
    {
      name: 'Lag',
      value: formatNumber(props.metrics.lag),
      tooltip:
        'Total number of events waiting to be delivered. Each subscription ' +
        'has a "natural" lag, which depends on production rate.',
    },
    {
      name: 'Output rate',
      tooltip:
        'Maximum sending rate calculated based on receiving service ' +
        'performance. For well-performing service output rate should be ' +
        'equal to rate limit.',
    },
  ];
</script>

<template>
  <key-value-card :entries="entries" card-title="Subscription metrics" />
</template>

<style scoped lang="scss"></style>
