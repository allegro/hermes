<script setup lang="ts">
  const props = defineProps<{
    name: string;
    value: any;
  }>();
</script>

<template>
  <div>
    <span class="font-weight-bold">{{ props.name }}: </span>
    <span>{{ props.value }}</span>
  </div>
</template>

<style scoped lang="scss"></style>
