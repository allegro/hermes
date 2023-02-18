<script setup lang="ts">
  import KeyValueCard from '@/components/key-value-card/KeyValueCard.vue';

  // TODO: proper links to dashboards
  const entries = [
    { name: '2xx', nameHref: 'https://example.com/' },
    { name: '4xx', nameHref: 'https://example.com/' },
    { name: '5xx', nameHref: 'https://example.com/' },
    { name: 'Network timeouts', nameHref: 'https://example.com/' },
    { name: 'Other network errors', nameHref: 'https://example.com/' },
  ];
</script>

<template>
  <key-value-card :entries="entries" card-title="Service response metrics" />
</template>

<style scoped lang="scss"></style>
