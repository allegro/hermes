<script setup lang="ts">
  import KeyValueCard from '@/components/key-value-card/KeyValueCard.vue';

  // TODO: data from API
  const entries = [
    { name: 'Delivery rate', value: '0.0' },
    {
      name: 'Subscriber latency',
      tooltip:
        'Latency of acknowledging messages by subscribing service as measured by Hermes.',
    },
    { name: 'Delivered', value: '39,095' },
    { name: 'Discarded', value: '1,369,364' },
    {
      name: 'Lag',
      value: '2,378',
      tooltip:
        'Total number of events waiting to be delivered. Each subscription has a "natural" lag, which depends on production rate.',
    },
    {
      name: 'Output rate',
      tooltip:
        'Maximum sending rate calculated based on receiving service performance. For well-performing service output rate should be equal to rate limit.',
    },
  ];
</script>

<template>
  <key-value-card :entries="entries" card-title="Subscription metrics" />
</template>

<style scoped lang="scss"></style>
