<script setup lang="ts">
  import { formatTimestamp } from '@/utils/date-formatter/date-formatter';
  import { useI18n } from 'vue-i18n';
  import KeyValueCard from '@/components/key-value-card/KeyValueCard.vue';
  import type { SentMessageTrace } from '@/api/subscription-undelivered';

  const props = defineProps<{
    lastUndelivered: SentMessageTrace;
  }>();

  const { t } = useI18n();

  const entries = [
    {
      name: t('subscription.lastUndeliveredMessage.time'),
      value: formatTimestamp(props.lastUndelivered.timestamp),
    },
    {
      name: t('subscription.lastUndeliveredMessage.reason'),
      value: props.lastUndelivered.reason,
    },
    {
      name: t('subscription.lastUndeliveredMessage.message'),
      value: props.lastUndelivered.message,
    },
  ];
</script>

<template>
  <key-value-card
    :entries="entries"
    :card-title="t('subscription.lastUndeliveredMessage.title')"
  />
</template>

<style scoped lang="scss"></style>
