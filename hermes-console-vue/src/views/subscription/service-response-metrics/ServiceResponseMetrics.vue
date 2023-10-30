<script setup lang="ts">
  import KeyValueCard from '@/components/key-value-card/KeyValueCard.vue';
  import KeyValueCardItem from '@/components/key-value-card/key-value-card-item/KeyValueCardItem.vue';

  // TODO: proper links to dashboards
</script>

<template>
  <key-value-card :title="$t('subscription.serviceResponseMetrics.title')">
    <key-value-card-item
      :name="$t('subscription.serviceResponseMetrics.2xx')"
      name-href="https://example.com/"
    />
    <key-value-card-item
      :name="$t('subscription.serviceResponseMetrics.4xx')"
      name-href="https://example.com/"
    />
    <key-value-card-item
      :name="$t('subscription.serviceResponseMetrics.5xx')"
      name-href="https://example.com/"
    />
    <key-value-card-item
      :name="$t('subscription.serviceResponseMetrics.networkTimeouts')"
      name-href="https://example.com/"
    />
    <key-value-card-item
      :name="$t('subscription.serviceResponseMetrics.otherNetworkErrors')"
      name-href="https://example.com/"
    />
  </key-value-card>
</template>

<style scoped lang="scss"></style>
