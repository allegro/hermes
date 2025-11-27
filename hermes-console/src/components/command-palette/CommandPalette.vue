<script setup lang="ts">
  import { computed } from 'vue';
  import CommandPaletteItem from '@/components/command-palette/command-palette-item/CommandPaletteItem.vue';
  import type { CommandPaletteElement } from '@/components/command-palette/types';

  const props = defineProps<{
    items: CommandPaletteElement[];
    search: string;
    loading: boolean;
    modelValue: boolean;
  }>();

  const emit = defineEmits<{
    (e: 'update:modelValue', value: boolean): void;
    (e: 'update:search', value: string): void;
  }>();

  const isOpen = computed({
    get: () => props.modelValue,
    set: (value: boolean) => emit('update:modelValue', value),
  });

  const _search = computed({
    get: () => props.search,
    set: (value: string) => emit('update:search', value),
  });
</script>

<template>
  <v-dialog
    v-model="isOpen"
    max-width="900"
    min-width="900"
    class="command-palette-dialog"
  >
    <v-card class="command-palette-card">
      <v-card-title class="pa-0">
        <v-text-field
          v-model="_search"
          autofocus
          variant="solo"
          placeholder="Search topics and subscriptions"
          prepend-inner-icon="mdi-magnify"
          hide-details
        />
      </v-card-title>

      <v-divider />

      <v-card-text class="pa-0 command-palette-results">
        <v-progress-linear v-if="loading" indeterminate color="primary" />

        <div v-if="!loading && items.length === 0 && _search.length > 0">
          <p class="pa-4 text-medium-emphasis">No results</p>
        </div>

        <template v-else>
          <v-list density="compact">
            <template v-for="item in items" :key="item.id">
              <v-list-subheader v-if="item.type === 'title'">
                {{ item.title }}
              </v-list-subheader>

              <v-divider v-else-if="item.type === 'divider'" />

              <command-palette-item
                v-else-if="item.type === 'item'"
                :title="item.title"
                :subtitle="item.subtitle"
                :icon="item.icon"
                :label="item.label"
                :label-color="item.labelColor"
                @click="item.onClick"
              />
            </template>
          </v-list>
        </template>
      </v-card-text>

      <v-divider />

      <v-card-actions class="justify-end">
        <span class="text-caption text-medium-emphasis mr-4">
          {{ items.length }} results
        </span>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<style scoped lang="scss">
  .command-palette-dialog {
    align-items: flex-start;
    justify-content: center;
    padding-top: 10vh;
  }

  .command-palette-card {
    width: 100%;
  }

  .command-palette-results {
    max-height: 50vh;
    overflow-y: auto;
  }
</style>
