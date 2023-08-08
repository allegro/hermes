<script setup lang="ts">
  import { formatNumber } from '@/utils/number-formatter/number-formatter';
  import KeyValueCard from '@/components/key-value-card/KeyValueCard.vue';
  import KeyValueCardItem from '@/components/key-value-card/key-value-card-item/KeyValueCardItem.vue';
  import type { SubscriptionMetrics } from '@/api/subscription-metrics';

  const props = defineProps<{
    subscriptionMetrics: SubscriptionMetrics;
  }>();
</script>

<template>
  <key-value-card :title="$t('subscription.metricsCard.title')">
    <key-value-card-item
      :name="$t('subscription.metricsCard.deliveryRate')"
      :value="formatNumber(props.subscriptionMetrics.rate, 2)"
    />
    <key-value-card-item
      :name="$t('subscription.metricsCard.subscriberLatency')"
      value="?"
      :tooltip="$t('subscription.metricsCard.tooltips.subscriberLatency')"
    />
    <key-value-card-item
      :name="$t('subscription.metricsCard.delivered')"
      :value="formatNumber(props.subscriptionMetrics.delivered)"
    />
    <key-value-card-item
      :name="$t('subscription.metricsCard.discarded')"
      :value="formatNumber(props.subscriptionMetrics.discarded)"
    />
    <key-value-card-item
      :name="$t('subscription.metricsCard.lag')"
      :value="formatNumber(props.subscriptionMetrics.lag)"
      :tooltip="$t('subscription.metricsCard.tooltips.lag')"
    />
    <key-value-card-item
      :name="$t('subscription.metricsCard.outputRate')"
      value="?"
      :tooltip="$t('subscription.metricsCard.tooltips.outputRate')"
    />
  </key-value-card>
</template>

<style scoped lang="scss"></style>
