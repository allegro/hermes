<script setup lang="ts">
  import { formatNumber } from '@/utils/number-formatter/number-formatter';
  import { useI18n } from 'vue-i18n';
  import type { ConsumerGroupMember } from '@/api/consumer-group';
  const { t } = useI18n();

  const props = defineProps<{
    members: ConsumerGroupMember[];
  }>();
</script>

<template>
  <v-table>
    <thead>
      <tr>
        <th class="text-left">
          {{ t('consumerGroups.listing.type') }}
        </th>
        <th class="text-left">
          {{ t('consumerGroups.listing.partition') }}
        </th>
        <th class="text-left">
          {{ t('consumerGroups.listing.currentOffset') }}
        </th>
        <th class="text-left">
          {{ t('consumerGroups.listing.endOffset') }}
        </th>
        <th class="text-left">
          {{ t('consumerGroups.listing.lag') }}
        </th>
      </tr>
    </thead>
    <tbody v-for="item in props.members" :key="item">
      <tr>
        <td colspan="5">
          <strong
            >{{ t('consumerGroups.listing.host') }} {{ item.host }}</strong
          >
        </td>
      </tr>
      <tr v-for="partition in item.partitions" :key="partition">
        <td v-if="partition.contentType === 'AVRO'">
          <v-chip
            color="green"
            size="default"
            density="comfortable"
            variant="flat"
          >
            {{ partition.contentType }}
          </v-chip>
        </td>
        <td v-else>
          <v-chip
            color="orange"
            size="default"
            density="comfortable"
            variant="flat"
          >
            {{ partition.contentType }}
          </v-chip>
        </td>
        <td>{{ partition.partition }}</td>
        <td>{{ formatNumber(partition.currentOffset) }}</td>
        <td>{{ formatNumber(partition.logEndOffset) }}</td>
        <td>{{ formatNumber(partition.lag) }}</td>
      </tr>
    </tbody>
  </v-table>
</template>

<style scoped lang="scss"></style>
