<script setup lang="ts">
  import { v4 as generateUUID } from 'uuid';
  import { ref } from 'vue';
  import PathFilterRow from '@/views/subscription/subscription-form/subscription-basic-filters/path-filter-row/PathFilterRow.vue';
  import type { FilterMatchingStrategy, PathFilter } from './types';

  const props = defineProps<{
    modelValue: PathFilter[];
    paths: string[];
  }>();
  const emit = defineEmits(['update:modelValue']);

  const newFilterPath = ref('');
  const newFilterMatcher = ref('');
  const newFilterMatchingStrategy = ref<FilterMatchingStrategy>('all');

  function addFilter() {
    const newFilter = {
      id: generateUUID(),
      path: newFilterPath.value,
      matcher: newFilterMatcher.value,
      matchingStrategy: newFilterMatchingStrategy.value,
    };
    const updatedFilters = props.modelValue.concat([newFilter]);
    newFilterPath.value = '';
    newFilterMatcher.value = '';
    newFilterMatchingStrategy.value = 'all';
    emit('update:modelValue', updatedFilters);
  }

  function removeFilter(filterToRemove: PathFilter) {
    const updatedFilters = props.modelValue.filter(
      (filter) => filter !== filterToRemove,
    );
    emit('update:modelValue', updatedFilters);
  }
</script>

<template>
  <path-filter-row
    v-for="filter in props.modelValue"
    :key="filter.id"
    :paths="paths"
    v-model:path="filter.path"
    v-model:matcher="filter.matcher"
    v-model:matching-strategy="filter.matchingStrategy"
    @remove="removeFilter(filter)"
    type="created"
  />

  <path-filter-row
    :paths="paths"
    v-model:path="newFilterPath"
    v-model:matcher="newFilterMatcher"
    v-model:matchingStrategy="newFilterMatchingStrategy"
    @add="addFilter()"
    type="new"
  />
</template>

<style scoped lang="scss"></style>
