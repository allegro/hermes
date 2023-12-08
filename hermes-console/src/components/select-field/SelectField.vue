<script setup lang="ts">
  import type { FieldValidator } from '@/utils/validators';

  const props = defineProps<{
    label: string;
    items: any[];
    modelValue?: any[] | any;
    rules?: FieldValidator<any>[];
  }>();
  defineEmits(['update:modelValue']);
</script>

<template>
  <v-select
    :model-value="props.modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    :rules="props.rules || []"
    :label="props.label"
    density="comfortable"
    :items="props.items"
    variant="outlined"
    persistent-placeholder
  />
</template>

<style scoped lang="scss"></style>
