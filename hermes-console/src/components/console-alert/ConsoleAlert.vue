<script setup lang="ts">
  const props = defineProps<{
    icon?: string;
    title?: string;
    text: string;
    type: 'error' | 'success' | 'warning' | 'info';
    link?: string;
    linkDescription?: string;
  }>();
</script>

<template>
  <v-alert
    :title="props.title ?? ''"
    :text="props.text"
    variant="elevated"
    :type="props.type"
    border="start"
    :icon="icon ?? `\$${type}`"
  >
    <a
      v-if="link != null && linkDescription != null"
      :href="link"
      target="_blank"
      >{{ linkDescription }}</a
    >
  </v-alert>
</template>

<style scoped lang="scss"></style>
