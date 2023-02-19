<script setup lang="ts">
  import { formatTimestamp } from '@/utils/date-formatter/date-formatter';
  import KeyValueCard from '@/components/key-value-card/KeyValueCard.vue';
  import type { SentMessageTrace } from '@/api/subscription-undelivered';

  const props = defineProps<{
    lastUndelivered: SentMessageTrace;
  }>();

  const entries = [
    {
      name: 'Time',
      value: formatTimestamp(props.lastUndelivered.timestamp),
    },
    {
      name: 'Reason',
      value: props.lastUndelivered.reason,
    },
    {
      name: 'Message',
      value: props.lastUndelivered.message,
    },
  ];
</script>

<template>
  <key-value-card :entries="entries" card-title="Last undelivered message" />
</template>

<style scoped lang="scss"></style>
