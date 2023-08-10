<script setup lang="ts">
  const props = defineProps<{
    label: string;
    items: any[];
  }>();
</script>

<template>
  <v-select
      :label="props.label"
      density="comfortable"
      :items="props.items"
      variant="outlined"
      persistent-placeholder
  />
</template>

<style scoped lang="scss">

</style>
