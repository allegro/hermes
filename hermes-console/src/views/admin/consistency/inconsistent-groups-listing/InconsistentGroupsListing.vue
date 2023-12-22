<script setup lang="ts">
  import { computed } from 'vue';
  import { useI18n } from 'vue-i18n';
  import { useRouter } from 'vue-router';
  import type { InconsistentGroup } from '@/api/inconsistent-group';

  const router = useRouter();
  const { t } = useI18n();

  const props = defineProps<{
    inconsistentGroups: InconsistentGroup[];
    filter?: string;
  }>();

  const filteredGroups = computed(() => {
    return props.inconsistentGroups.filter(
      (group) =>
        !props.filter ||
        group.name.toLowerCase().includes(props.filter.toLowerCase()),
    );
  });

  function onGroupClick(groupName: string) {
    router.push({ path: `/ui/consistency/${groupName}` });
  }
</script>

<template>
  <v-card class="mb-2">
    <v-table density="comfortable" hover>
      <thead>
        <tr>
          <th>{{ $t('consistency.inconsistentGroups.listing.index') }}</th>
          <th>{{ $t('consistency.inconsistentGroups.listing.name') }}</th>
        </tr>
      </thead>
      <tbody v-if="filteredGroups.length > 0">
        <tr
          v-for="(group, index) in filteredGroups"
          :key="group"
          @click="onGroupClick(group.name)"
        >
          <td class="text-medium-emphasis">
            {{ index + 1 }}
          </td>
          <td class="font-weight-medium">
            {{ group.name }}
          </td>
        </tr>
      </tbody>
      <tbody v-else>
        <tr>
          <th colspan="3" class="text-center text-medium-emphasis">
            {{ $t('consistency.inconsistentGroups.noGroups') }}
            <template v-if="filter">
              {{
                t('consistency.inconsistentGroups.appliedFilter', { filter })
              }}
            </template>
          </th>
        </tr>
      </tbody>
    </v-table>
  </v-card>
</template>
