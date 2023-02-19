<script setup lang="ts">
  import { useI18n } from 'vue-i18n';
  import KeyValueCard from '@/components/key-value-card/KeyValueCard.vue';

  const { t } = useI18n();

  // TODO: proper links to dashboards
  const entries = [
    {
      name: t('subscription.serviceResponseMetrics.2xx'),
      nameHref: 'https://example.com/',
    },
    {
      name: t('subscription.serviceResponseMetrics.4xx'),
      nameHref: 'https://example.com/',
    },
    {
      name: t('subscription.serviceResponseMetrics.5xx'),
      nameHref: 'https://example.com/',
    },
    {
      name: t('subscription.serviceResponseMetrics.networkTimeouts'),
      nameHref: 'https://example.com/',
    },
    {
      name: t('subscription.serviceResponseMetrics.otherNetworkErrors'),
      nameHref: 'https://example.com/',
    },
  ];
</script>

<template>
  <key-value-card
    :entries="entries"
    :card-title="t('subscription.serviceResponseMetrics.title')"
  />
</template>

<style scoped lang="scss"></style>
