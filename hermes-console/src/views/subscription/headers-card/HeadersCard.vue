<script setup lang="ts">
  import type { Header } from '@/api/subscription';

  const props = defineProps<{
    headers?: Header[];
  }>();
</script>

<template>
  <v-card class="mb-2">
    <v-card-item class="border-b">
      <div class="d-flex justify-space-between align-start">
        <div>
          <v-card-title class="font-weight-bold"
            >{{ $t('subscription.headersCard.title') }}
          </v-card-title>
          <v-card-subtitle
            >{{ $t('subscription.headersCard.subtitle') }}
          </v-card-subtitle>
        </div>
      </div>
    </v-card-item>

    <v-card-item>
      <v-table density="compact">
        <thead>
          <tr>
            <th class="text-left pl-0">
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
        <tbody v-if="props.headers && props.headers.length > 0">
          <tr v-for="(header, index) in props.headers" :key="index">
            <td class="pl-0">{{ index + 1 }}</td>
            <td>{{ header.name }}</td>
            <td>{{ header.value }}</td>
          </tr>
        </tbody>
        <tbody v-else>
          <tr>
            <td
              colspan="5"
              class="text-center text-medium-emphasis text-body-2"
            >
              <span>{{ $t('subscription.headersCard.noHeaders') }}</span>
            </td>
          </tr>
        </tbody>
      </v-table>
    </v-card-item>
  </v-card>
</template>

<style scoped lang="scss"></style>
