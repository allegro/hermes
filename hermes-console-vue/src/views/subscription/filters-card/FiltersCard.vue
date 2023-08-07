<script setup lang="ts">
  import type { MessageFilterSpecification } from '@/api/subscription';

  const props = defineProps<{
    filters: MessageFilterSpecification[];
  }>();
</script>

<template>
  <v-card class="mb-2">
    <template #title>
      <p class="font-weight-bold">
        {{ $t('subscription.filtersCard.title') }}
      </p>
    </template>
    <v-table density="compact">
      <thead>
        <tr>
          <th class="text-left">
            {{ $t('subscription.filtersCard.index') }}
          </th>
          <th class="text-left">
            {{ $t('subscription.filtersCard.type') }}
          </th>
          <th class="text-left">
            {{ $t('subscription.filtersCard.path') }}
          </th>
          <th class="text-left">
            {{ $t('subscription.filtersCard.matcher') }}
          </th>
          <th class="text-left">
            {{ $t('subscription.filtersCard.matchingStrategy') }}
          </th>
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
      <v-btn prepend-icon="mdi-console-line">
        {{ $t('subscription.filtersCard.debug') }}
      </v-btn>
    </template>
  </v-card>
</template>

<style scoped lang="scss"></style>
