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
    variant="outlined"
    density="comfortable"
    @click="open"
  >
    <span class="text-capitalize pr-4">Search</span>
    <span>cmd + k</span>
  </v-btn>
</template>

<style scoped lang="scss"></style>
