<script setup lang="ts">
  import { useI18n } from 'vue-i18n';
  import KeyValueCard from '@/components/key-value-card/KeyValueCard.vue';
  import KeyValueCardItem from '@/components/key-value-card/key-value-card-item/KeyValueCardItem.vue';

  const { t } = useI18n();
</script>

<template>
  <key-value-card :title="t('subscription.serviceResponseMetrics.title')">
    <key-value-card-item
      :name="t('subscription.serviceResponseMetrics.2xx')"
      name-href="https://example.com/"
    />
    <key-value-card-item
      :name="t('subscription.serviceResponseMetrics.4xx')"
      name-href="https://example.com/"
    />
    <key-value-card-item
      :name="t('subscription.serviceResponseMetrics.5xx')"
      name-href="https://example.com/"
    />
    <key-value-card-item
      :name="t('subscription.serviceResponseMetrics.networkTimeouts')"
      name-href="https://example.com/"
    />
    <key-value-card-item
      :name="t('subscription.serviceResponseMetrics.otherNetworkErrors')"
      name-href="https://example.com/"
    />
  </key-value-card>
</template>

<style scoped lang="scss"></style>
