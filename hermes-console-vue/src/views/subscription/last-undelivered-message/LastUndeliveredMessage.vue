<script setup lang="ts">
  import { formatTimestamp } from '@/utils/date-formatter/date-formatter';
  import KeyValueCard from '@/components/key-value-card/KeyValueCard.vue';
  import KeyValueCardItem from '@/components/key-value-card/key-value-card-item/KeyValueCardItem.vue';
  import type { SentMessageTrace } from '@/api/subscription-undelivered';

  const props = defineProps<{
    lastUndelivered: SentMessageTrace;
  }>();
</script>

<template>
  <key-value-card :title="$t('subscription.lastUndeliveredMessage.title')">
    <key-value-card-item
      :name="$t('subscription.lastUndeliveredMessage.time')"
      :value="formatTimestamp(props.lastUndelivered.timestamp)"
    />
    <key-value-card-item
      :name="$t('subscription.lastUndeliveredMessage.reason')"
      :value="props.lastUndelivered.reason"
    />
    <key-value-card-item
      :name="$t('subscription.lastUndeliveredMessage.message')"
      :value="props.lastUndelivered.message"
    />
  </key-value-card>
</template>

<style scoped lang="scss"></style>
