<script setup lang="ts">
  import type { Header } from '@/api/subscription';

  const props = defineProps<{
    headers: Header[];
  }>();
</script>

<template>
  <v-card class="mb-2">
    <template #title>
      <p class="font-weight-bold">
        {{ $t('subscription.headersCard.title') }}
      </p>
    </template>
    <v-table density="compact">
      <thead>
        <tr>
          <th class="text-left">
            {{ $t('subscription.headersCard.index') }}
          </th>
          <th class="text-left">
            {{ $t('subscription.headersCard.name') }}
          </th>
          <th class="text-left">
            {{ $t('subscription.headersCard.value') }}
          </th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(header, index) in props.headers" :key="index">
          <td>{{ index + 1 }}</td>
          <td>{{ header.name }}</td>
          <td>{{ header.value }}</td>
        </tr>
      </tbody>
    </v-table>
  </v-card>
</template>

<style scoped lang="scss"></style>
