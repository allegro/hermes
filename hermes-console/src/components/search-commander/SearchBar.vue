<script setup lang="ts">
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
    width="400"
    max-width="100%"
    spaced="both"
  >
    <span class="text-none">Search</span>
    <template #append>
      <v-hotkey
        keys="cmd+k"
        platform="auto"
        variant="text"
        color="grey-darken-2"
      />
    </template>
  </v-btn>
</template>

<style scoped lang="scss"></style>
