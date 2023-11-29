<script setup lang="ts">
  import { computed } from 'vue';
  import { useI18n } from 'vue-i18n';
  import { useRouter } from 'vue-router';
  import type { Group } from '@/composables/groups/use-groups/useGroups';

  const router = useRouter();
  const { t } = useI18n();

  const props = defineProps<{
    groups?: Group[];
    filter?: string;
  }>();

  const filteredGroups = computed(() => {
    return (props.groups ?? []).filter(
      (group) =>
        !props.filter ||
        group.name.toLowerCase().includes(props.filter.toLowerCase()),
    );
  });

  function onGroupClick(groupName: string) {
    router.push({ path: `/ui/groups/${groupName}` });
  }

  function onGroupBlankClick(groupName: string) {
    window.open(`${router.currentRoute.value.path}/${groupName}`, '_blank');
  }
</script>

<template>
  <v-card class="mb-2">
    <v-table density="comfortable" hover>
      <thead>
        <tr>
          <th>{{ t('groups.groupListing.index') }}</th>
          <th>{{ t('groups.groupListing.name') }}</th>
          <th></th>
        </tr>
      </thead>
      <tbody v-if="filteredGroups.length > 0">
        <tr
          v-for="(group, index) in filteredGroups"
          :key="group.name"
          class="groups-table__row"
          @click.exact="onGroupClick(group.name)"
          @click.ctrl="onGroupBlankClick(group.name)"
          @click.meta="onGroupBlankClick(group.name)"
          @contextmenu="onGroupBlankClick(group.name)"
        >
          <td class="text-medium-emphasis">
            {{ index + 1 }}
          </td>
          <td class="font-weight-medium">
            {{ group.name }}
          </td>
          <td class="text-right">
            <v-chip
              color="secondary"
              size="small"
              density="comfortable"
              variant="flat"
            >
              {{ t('groups.groupListing.topicsChip') }}
              {{ group.topics.length }}
            </v-chip>
            <v-icon icon="mdi-chevron-right"></v-icon>
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
