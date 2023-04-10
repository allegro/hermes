<script setup lang="ts">
  interface ItemEntry {
    title: string;
    href?: string;
  }

  const props = defineProps<{
    items: [ItemEntry];
  }>();
</script>

<template>
  <v-breadcrumbs :items="items" density="compact" />
</template>

<style scoped lang="scss"></style>
