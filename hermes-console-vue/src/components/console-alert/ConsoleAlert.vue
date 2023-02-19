<script setup lang="ts">
  const props = defineProps<{
    icon?: string;
    title?: string;
    text: string;
    type: 'error' | 'success' | 'warning' | 'info';
  }>();
</script>

<template>
  <v-alert
    :title="props.title"
    :text="props.text"
    variant="elevated"
    :type="props.type"
    border="start"
    :icon="icon ?? `\$${type}`"
  ></v-alert>
</template>

<style scoped lang="scss"></style>
