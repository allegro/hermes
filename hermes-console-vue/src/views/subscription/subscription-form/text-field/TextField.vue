<script setup lang="ts">
  const props = defineProps<{
    label: string;
    placeholder?: string;
    autofocus?: boolean;
    suffix?: string;
    modelValue: string;
  }>();
  defineEmits(['update:modelValue']);
</script>

<template>
  <v-text-field
      :value="modelValue"
      @input="$emit('update:modelValue', $event.target.value)"
      :label="props.label"
      :placeholder="props.placeholder ?? ''"
      density="comfortable"
      :autofocus="autofocus ?? false"
      variant="outlined"
      :persistent-placeholder="true"
      :suffix="suffix ?? ''"
  />
</template>

<style scoped lang="scss">

</style>
