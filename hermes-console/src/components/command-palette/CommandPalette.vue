<script setup lang="ts">
  import { computed } from 'vue';
  import CommandPaletteItem from '@/components/command-palette/command-palette-item/CommandPaletteItem.vue';
  import type { CommandPaletteElement } from '@/components/command-palette/types';

  const {
    items,
    numberOfResults,
    search,
    loading,
    modelValue,
    inputPlaceholder,
  } = defineProps<{
    items: CommandPaletteElement[];
    numberOfResults: number;
    search: string;
    loading: boolean;
    modelValue: boolean;
    inputPlaceholder?: string;
  }>();

  const emit = defineEmits<{
    (e: 'update:modelValue', value: boolean): void;
    (e: 'update:search', value: string): void;
  }>();

  const isOpen = computed({
    get: () => modelValue,
    set: (value: boolean) => emit('update:modelValue', value),
  });

  const _search = computed({
    get: () => search,
    set: (value: string) => emit('update:search', value),
  });
</script>

<template>
  <v-dialog
    v-model="isOpen"
    width="60rem"
    min-height="50vh"
    max-height="90vh"
    class="command-palette-dialog"
  >
    <v-card class="command-palette-card" height="500">
      <v-card-title class="pa-0">
        <v-text-field
          v-model="_search"
          autofocus
          variant="solo"
          :placeholder="inputPlaceholder"
          prepend-inner-icon="mdi-magnify"
          hide-details
          data-testid="command-palette-search-input"
        />
      </v-card-title>

      <v-divider />

      <v-card-text class="pa-0 command-palette-results d-flex flex-column">
        <v-progress-linear v-if="loading" indeterminate color="primary" />

        <div
          v-if="!loading && items.length === 0 && _search.length > 0"
          class="text-center empty-state"
        >
          <p class="pa-4 text-medium-emphasis">
            {{ $t('commandPalette.noResults') }}
          </p>
        </div>

        <div
          v-else-if="!loading && items.length === 0 && _search.length === 0"
          class="text-center empty-state"
        >
          <p class="pa-4 text-medium-emphasis">
            {{ $t('commandPalette.searchIncentive') }}
          </p>
        </div>

        <!-- It's IMPORTANT for performance reasons to have max-height on virtual-scroll and d-flex on parent node -->
        <v-virtual-scroll
          v-else
          density="compact"
          :items="items"
          hide-default-header
          max-height="500"
        >
          <template #default="{ item }">
            <div :key="item.id" data-testid="command-palette-element">
              <v-list-subheader
                v-if="item.type === 'subheader'"
                :key="item.id"
                class="px-3"
              >
                {{ item.title }}
              </v-list-subheader>

              <v-divider
                v-else-if="item.type === 'divider'"
                class="mt-3 mb-2"
                data-testid="command-palette-divider-element"
              />

              <command-palette-item
                v-else-if="item.type === 'item'"
                :title="item.title"
                :subtitle="item.subtitle"
                :icon="item.icon"
                :label="item.label"
                :label-color="item.labelColor"
                @click="item.onClick"
                class="py-2"
              />
            </div>
          </template>
        </v-virtual-scroll>
      </v-card-text>

      <v-divider />

      <v-card-actions class="justify-end">
        <span class="text-caption text-medium-emphasis mr-4">
          {{ numberOfResults }} {{ $t('commandPalette.resultsCounts') }}
        </span>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<style scoped lang="scss">
  .command-palette-card {
    opacity: 0.96;
  }

  .command-palette-results {
    overflow-y: auto;
  }

  .empty-state {
    display: flex;
    align-items: center;
    justify-content: center;
    min-height: 10rem;
  }
</style>
