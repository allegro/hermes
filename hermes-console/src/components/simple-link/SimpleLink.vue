<script setup lang="ts">
  const props = defineProps<{
    href: string;
    text: string;
    openInNewTab?: boolean;
    lineHeight?: string;
  }>();
</script>

<template>
  <a
    :href="props.href"
    :target="props.openInNewTab ? '_blank' : '_self'"
    class="text-decoration-none text-button text-none text-primary"
  >
    <span>{{ props.text }}</span>
    <v-icon
      v-if="props.openInNewTab"
      color="primary"
      size="x-small"
      class="ml-1"
      >mdi-open-in-new
    </v-icon>
  </a>
</template>

<style scoped lang="scss"></style>
