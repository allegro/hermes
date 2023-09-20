<script setup lang="ts">
  import { v4 as generateUUID } from 'uuid';
  import { PathFilter } from '@/views/subscription/subscription-form/subscription-basic-filters/types';
  import SubscriptionPathFiltersDebug from '@/views/subscription/subscription-form/subscription-basic-filters/SubscriptionPathFiltersDebug.vue';
  import type { MessageFilterSpecification } from '@/api/subscription';

  const props = defineProps<{
    topic: string;
    filters: MessageFilterSpecification[];
  }>();
  const copy = (filters: MessageFilterSpecification[]): PathFilter[] => {
    return filters.map((f) => mapFilter(f));
  };

  const mapFilter = (
    messageFilterSpec: MessageFilterSpecification,
  ): PathFilter => {
    return {
      id: generateUUID(),
      path: messageFilterSpec.path,
      matcher: messageFilterSpec.matcher,
      matchingStrategy: messageFilterSpec.matchingStrategy,
    };
  };
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
        <tr v-for="(filter, index) in props.filters ?? []" :key="index">
          <td>{{ index + 1 }}</td>
          <td>{{ filter.type }}</td>
          <td>{{ filter.path }}{{ filter.header }}</td>
          <td>{{ filter.matcher }}</td>
          <td>{{ filter.matchingStrategy }}</td>
        </tr>
      </tbody>
    </v-table>
    <template #actions>
      <subscription-path-filters-debug
        :topic="props.topic"
        :model-value="copy(props.filters)"
        :edit-enabled="false"
      />
    </template>
  </v-card>
</template>

<style scoped lang="scss"></style>
