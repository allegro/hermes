<script setup lang="ts">
  import { v4 as generateUUID } from 'uuid';
  import { ref } from 'vue';
  import BasicFilterRow from '@/views/subscription/subscription-form/subscription-basic-filters/basic-filter-row/BasicFilterRow.vue';
  import type { Filter, FilterMatchingStrategy } from './types';

  const filters = ref<Filter[]>([]);
  const newFilterPath = ref('');
  const newFilterMatcher = ref('');
  const newFilterMatchingStrategy = ref<FilterMatchingStrategy>('all');

  function addFilter() {
    filters.value.push({
      id: generateUUID(),
      path: newFilterPath.value,
      matcher: newFilterMatcher.value,
      matchingStrategy: newFilterMatchingStrategy.value,
    });
    newFilterPath.value = '';
    newFilterMatcher.value = '';
    newFilterMatchingStrategy.value = 'all';
  }

  function removeFilter(id: string) {
    const indexOfElementToRemove = filters.value.findIndex(
      (filter) => filter.id === id,
    );
    filters.value.splice(indexOfElementToRemove, 1);
  }
</script>

<template>
  <span class="text-subtitle-1 mb-2">Filters</span>
  <basic-filter-row
    v-for="filter in filters"
    :key="filter.id"
    v-model:path="filter.path"
    v-model:matcher="filter.matcher"
    v-model:matching-strategy="filter.matchingStrategy"
    @remove="removeFilter(filter.id)"
    type="created"
  />

  <basic-filter-row
    v-model:path="newFilterPath"
    v-model:matcher="newFilterMatcher"
    v-model:matchingStrategy="newFilterMatchingStrategy"
    @add="addFilter()"
    type="new"
  />
</template>

<style scoped lang="scss"></style>
