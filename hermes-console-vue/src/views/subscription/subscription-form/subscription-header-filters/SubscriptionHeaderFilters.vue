<script setup lang="ts">
  import { v4 as generateUUID } from 'uuid';
  import { ref } from 'vue';
  import HeaderFilterRow from '@/views/subscription/subscription-form/subscription-header-filters/header-filter-row/HeaderFilterRow.vue';
  import type { HeaderFilter } from '@/views/subscription/subscription-form/subscription-header-filters/types';

  const filters = ref<HeaderFilter[]>([]);
  const newFilterHeaderName = ref('');
  const newFilterMatcher = ref('');

  function addFilter() {
    filters.value.push({
      id: generateUUID(),
      name: newFilterHeaderName.value,
      matcher: newFilterMatcher.value,
    });
    newFilterHeaderName.value = '';
    newFilterMatcher.value = '';
  }

  function removeFilter(id: string) {
    const indexOfElementToRemove = filters.value.findIndex(
      (filter) => filter.id === id,
    );
    filters.value.splice(indexOfElementToRemove, 1);
  }
</script>
<template>
  <span class="text-subtitle-1 mb-2">HTTP header filters</span>
  <header-filter-row
    v-for="filter in filters"
    :key="filter.id"
    v-model:name="filter.name"
    v-model:matcher="filter.matcher"
    @remove="removeFilter(filter.id)"
    type="created"
  />

  <header-filter-row
    v-model:name="newFilterHeaderName"
    v-model:matcher="newFilterMatcher"
    @add="addFilter()"
    type="new"
  />
</template>

<style scoped lang="scss"></style>
