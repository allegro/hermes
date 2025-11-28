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
  >
    <span class="text-none">Search</span>
    <template #append>
      <v-hotkey
        :keys="hotKey"
        platform="auto"
        variant="text"
        color="grey-darken-2"
      />
    </template>
  </v-btn>
</template>

<style scoped lang="scss"></style>
