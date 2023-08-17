<script setup lang="ts">
  import type { FieldValidator } from '@/utils/validators';

  const props = defineProps<{
    label: string;
    placeholder?: string;
    autofocus?: boolean;
    suffix?: string;
    modelValue?: string | number;
    type?: string;
    rules?: FieldValidator<any>[];
  }>();
  defineEmits(['update:modelValue']);
</script>

<template>
  <v-text-field
    :value="props.modelValue"
    @input="$emit('update:modelValue', $event.target.value)"
    :type="props.type || 'string'"
    :rules="props.rules || []"
    :label="props.label"
    :placeholder="props.placeholder ?? ''"
    density="comfortable"
    :autofocus="props.autofocus ?? false"
    variant="outlined"
    :persistent-placeholder="true"
    :suffix="props.suffix ?? ''"
  />
</template>

<style scoped lang="scss"></style>
