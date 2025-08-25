<script setup lang="ts">
  import { v4 as generateUUID } from 'uuid';
  import { ref } from 'vue';
  import HeaderFilterRow from '@/views/subscription/subscription-form/subscription-header-filters/header-filter-row/HeaderFilterRow.vue';
  import type { HeaderFilter } from '@/views/subscription/subscription-form/subscription-header-filters/types';

  const props = defineProps<{
    modelValue: HeaderFilter[];
  }>();
  const emit = defineEmits(['update:modelValue']);

  const newFilterName = ref('');
  const newFilterValue = ref('');
  function addFilter() {
    const newFilter = {
      id: generateUUID(),
      name: newFilterName.value,
      value: newFilterValue.value,
    };
    const updatedFilters = props.modelValue.concat([newFilter]);
    newFilterName.value = '';
    newFilterValue.value = '';
    emit('update:modelValue', updatedFilters);
  }

  function removeFilter(filterToRemove: HeaderFilter) {
    const updatedFilters = props.modelValue.filter(
      (filter) => filter !== filterToRemove,
    );
    emit('update:modelValue', updatedFilters);
  }
</script>
<template>
  <span class="text-subtitle-1 mb-2">HTTP header filters</span>
  <header-filter-row
    v-for="filter in props.modelValue"
    :key="filter.id"
    v-model:name="filter.name"
    v-model:value="filter.value"
    @remove="removeFilter(filter)"
    type="created"
  />

  <header-filter-row
    v-model:name="newFilterName"
    v-model:value="newFilterValue"
    @add="addFilter()"
    type="new"
    class="mb-3"
  />
</template>

<style scoped lang="scss"></style>
