<script setup lang="ts">
  import { useI18n } from 'vue-i18n';
  import type {InactiveTopic} from "@/api/inactive-topics";
  const { t } = useI18n();

  const props = defineProps<{
    inactiveTopics: InactiveTopic[]
  }>();

</script>

<template>
  <v-card class="mb-2">
    <v-table density="comfortable" hover>
      <thead>
        <tr>
          <th>{{ $t('inactiveTopics.listing.index') }}</th>
          <th>{{ $t('inactiveTopics.listing.name') }}</th>
          <th>{{ $t('inactiveTopics.listing.lastUsed') }}</th>
          <th>{{ $t('inactiveTopics.listing.lastNotified') }}</th>
          <th>{{ $t('inactiveTopics.listing.howManyTimesNotified') }}</th>
          <th>{{ $t('inactiveTopics.listing.whitelisted') }}</th>
          <th></th>
        </tr>
      </thead>
      <tbody v-if="inactiveTopics.length > 0">
        <tr
          v-for="topic in inactiveTopics"
          :key="topic.name"
          class="inactive-topics-table__row"
        >
          <td class="text-medium-emphasis">
            1
          </td>
          <td class="font-weight-medium">
            {{ topic.topic }}
          </td>
          <td class="font-weight-medium">
            {{ new Date(topic.lastPublishedTsMs) }}
          </td>
          <td class="font-weight-medium">
            {{ ((topic.notificationTsMs.length > 0) ? new Date(Math.max.apply(Math, topic.notificationTsMs)) : '')}}
          </td>
          <td class="font-weight-medium">
            {{ topic.notificationTsMs.length }}
          </td>
          <td class="font-weight-medium">
            {{ topic.whitelisted }}
          </td>
        </tr>
      </tbody>
      <tbody v-else>
        <tr>
          <th colspan="3" class="text-center text-medium-emphasis">
            {{ $t('inactiveTopics.listing.noInactiveTopics') }}
          </th>
        </tr>
      </tbody>
    </v-table>
  </v-card>
</template>

<style scoped lang="scss">
  .inactive-topics-table__row:hover {
    cursor: pointer;
  }
</style>
