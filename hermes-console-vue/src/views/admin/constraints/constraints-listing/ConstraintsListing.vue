<script setup lang="ts">
  import { computed } from 'vue';
  import { useI18n } from 'vue-i18n';
  import type { Constraint } from '@/api/constraints';
  const { t } = useI18n();

  const props = defineProps<{
    constraints: Record<string, Constraint>;
    filter?: string;
  }>();

  const filteredConstraints = computed(() =>
    Object.fromEntries(
      Object.entries(props.constraints).filter(
        ([name]) => !props.filter || name.indexOf(props.filter) !== -1,
      ),
    ),
  );
</script>

<template>
  <v-card class="mb-2">
    <v-table density="comfortable" hover>
      <thead>
        <tr>
          <th>{{ $t('constraints.listing.index') }}</th>
          <th>{{ $t('constraints.listing.name') }}</th>
          <th></th>
        </tr>
      </thead>
      <tbody v-if="Object.entries(filteredConstraints).length > 0">
        <tr
          v-for="([key, value], index) in Object.entries(filteredConstraints)"
          :key="key"
          class="constraints-table__row"
        >
          <td class="text-medium-emphasis">
            {{ index + 1 }}
          </td>
          <td class="font-weight-medium">
            {{ key }}
          </td>
          <td class="text-right">
            <v-chip
              color="secondary"
              size="small"
              density="comfortable"
              variant="flat"
            >
              {{ $t('constraints.listing.consumersNumberChip') }}
              {{ value.consumersNumber }}
            </v-chip>
            <v-icon icon="mdi-chevron-right"></v-icon>
          </td>
        </tr>
      </tbody>
      <tbody v-else>
        <tr>
          <th colspan="3" class="text-center text-medium-emphasis">
            {{ $t('constraints.listing.noConstraints') }}
            <template v-if="filter">
              {{ t('constraints.listing.appliedFilter', { filter }) }}
            </template>
          </th>
        </tr>
      </tbody>
    </v-table>
  </v-card>
</template>

<style scoped lang="scss">
  .constraints-table__row:hover {
    cursor: pointer;
  }
</style>
