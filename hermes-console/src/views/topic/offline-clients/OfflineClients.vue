<script setup lang="ts">
  const props = defineProps<{
    source: string;
  }>();
</script>

<template>
  <iframe height="400" style="border: none" width="100%" :src="props.source">
  </iframe>
</template>

<style scoped lang="scss"></style>
