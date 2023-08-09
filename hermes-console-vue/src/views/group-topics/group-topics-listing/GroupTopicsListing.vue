<script setup lang="ts">
import { computed } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRouter } from 'vue-router';
import type { Group } from '@/composables/use-groups/useGroups';

const router = useRouter();
const { t } = useI18n();

const props = defineProps<{
  group?: Group;
  filter?: string;
}>();

const filteredTopics = computed(() => {
  return (props.group?.topics ?? []).filter(
      (topic) => !props.filter || topic.includes(props.filter),
  );
});

function onTopicClick(topicName: string) {
  router.push({ path: `/groups/${props.group?.name}/topics/${topicName}` });
}
</script>

<template>
  <v-card class="mb-2">
    <v-table density="comfortable" hover>
      <thead>
      <tr>
        <th>{{ t('groups.groupListing.index') }}</th>
        <th>{{ t('groups.groupListing.name') }}</th>
      </tr>
      </thead>
      <tbody v-if="filteredTopics.length > 0">
      <tr
          v-for="(topic, index) in filteredTopics"
          :key="topic"
          class="groups-table__row"
          @click="onTopicClick(topic)"
      >
        <td class="text-medium-emphasis">
          {{ index + 1 }}
        </td>
        <td class="font-weight-medium">
          {{ topic }}
        </td>
      </tr>
      </tbody>
      <tbody v-else>
      <tr>
        <th colspan="3" class="text-center text-medium-emphasis">
          {{ t('groups.groupListing.noGroups') }}
          <template v-if="filter">
            {{ t('groups.groupListing.appliedFilter', { filter }) }}
          </template>
        </th>
      </tr>
      </tbody>
    </v-table>
  </v-card>
</template>

<style scoped lang="scss">
.groups-table__row:hover {
  cursor: pointer;
}
</style>
