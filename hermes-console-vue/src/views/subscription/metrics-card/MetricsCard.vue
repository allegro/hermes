<script setup lang="ts">
  import { formatNumber } from '@/utils/number-formatter/number-formatter';
  import { useI18n } from 'vue-i18n';
  import KeyValueCard from '@/components/key-value-card/KeyValueCard.vue';
  import type { SubscriptionMetrics } from '@/api/subscription-metrics';

  const props = defineProps<{
    subscriptionMetrics: SubscriptionMetrics;
  }>();

  const { t } = useI18n();

  const entries = [
    {
      name: t('subscription.metricsCard.deliveryRate'),
      value: formatNumber(props.subscriptionMetrics.rate, 2),
    },
    {
      name: t('subscription.metricsCard.subscriberLatency'),
      tooltip: t('subscription.metricsCard.tooltips.subscriberLatency'),
    },
    {
      name: t('subscription.metricsCard.delivered'),
      value: formatNumber(props.subscriptionMetrics.delivered),
    },
    {
      name: t('subscription.metricsCard.discarded'),
      value: formatNumber(props.subscriptionMetrics.discarded),
    },
    {
      name: t('subscription.metricsCard.lag'),
      value: formatNumber(props.subscriptionMetrics.lag),
      tooltip: t('subscription.metricsCard.tooltips.lag'),
    },
    {
      name: t('subscription.metricsCard.outputRate'),
      tooltip: t('subscription.metricsCard.tooltips.outputRate'),
    },
  ];
</script>

<template>
  <key-value-card
    :entries="entries"
    :card-title="t('subscription.metricsCard.title')"
  />
</template>

<style scoped lang="scss"></style>
