<script setup lang="ts">
  const { hotKey } = defineProps<{
    hotKey: string;
  }>();

  const emit = defineEmits<{
    (e: 'open'): void;
  }>();

  function open() {
    emit('open');
  }
</script>

<template>
  <v-btn
    prepend-icon="mdi-magnify"
    variant="tonal"
    @click="open"
    class="text-medium-emphasis text-capitalize"
    width="500"
    max-width="100%"
    spaced="end"
    data-testid="search-bar-button"
  >
    <span class="text-none">Search</span>
    <template #append>
      <v-hotkey
        :keys="hotKey"
        platform="auto"
        variant="text"
        color="grey-darken-1"
        class="text-grey-darken-1"
      />
    </template>
  </v-btn>
</template>

<style scoped lang="scss"></style>
