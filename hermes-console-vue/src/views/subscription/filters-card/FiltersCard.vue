<script setup lang="ts">
  import type { MessageFilterSpecification } from '@/api/subscription';

  interface FiltersCardProps {
    filters: MessageFilterSpecification[];
  }

  const props = defineProps<FiltersCardProps>();
</script>

<template>
  <v-card class="mb-2">
    <template #title>
      <p class="font-weight-bold">Subscription message filters</p>
    </template>
    <v-table density="compact">
      <thead>
        <tr>
          <th class="text-left">#</th>
          <th class="text-left">Type</th>
          <th class="text-left">Path</th>
          <th class="text-left">Matcher</th>
          <th class="text-left">Matching strategy</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(filter, index) in props.filters" :key="index">
          <td>{{ index + 1 }}</td>
          <td>{{ filter.type }}</td>
          <td>{{ filter.path }}{{ filter.header }}</td>
          <td>{{ filter.matcher }}</td>
          <td>{{ filter.matchingStrategy }}</td>
        </tr>
      </tbody>
    </v-table>
    <template #actions>
      <v-btn prepend-icon="mdi-console-line">Debug</v-btn>
    </template>
  </v-card>
</template>

<style scoped lang="scss"></style>
