<script setup lang="ts">
  import { computed, ref, watch } from 'vue';
  import { search } from '@/api/hermes-client';
  import { useRouter } from 'vue-router';
  import CommandPeletteItem from '@/components/command-palette/command-palette-item/CommandPeletteItem.vue';
  import type { SearchResultItem, SearchResults } from '@/api/SearchResults';
  import type { CommandPaletteElement } from '@/components/command-palette/types';

  const props = defineProps<{
    modelValue: boolean;
  }>();

  const emit = defineEmits<{
    (e: 'update:modelValue', value: boolean): void;
  }>();

  const router = useRouter();

  const term = ref('');
  const loading = ref(false);
  const results = ref<SearchResultItem[]>([]);
  const totalCount = ref(0);

  const isOpen = computed({
    get: () => props.modelValue,
    set: (value: boolean) => emit('update:modelValue', value),
  });

  const groupedResults = computed(() => {
    const groups: Record<string, SearchResultItem[]> = {};

    for (const item of results.value) {
      if (!groups[item.type]) {
        groups[item.type] = [];
      }
      groups[item.type].push(item);
    }

    return Object.entries(groups).map(([type, items]) => ({
      type,
      items,
      label: type,
    }));
  });

  const getTypeColor = (type: string): string => {
    switch (type) {
      case 'TOPIC':
        return 'red';
      case 'SUBSCRIPTION':
        return 'secondary';
      default:
        return 'grey';
    }
  };

  const getTypeIcon = (type: string): string => {
    switch (type) {
      case 'TOPIC':
        return 'mdi-book-open-page-variant';
      case 'SUBSCRIPTION':
        return 'mdi-rss';
      default:
        return 'mdi-help-circle-outline';
    }
  };

  const items = computed<CommandPaletteElement[]>(() => {
    const flat: CommandPaletteElement[] = [];

    groupedResults.value.forEach((section) => {
      const sectionId = `section-${section.type}`;

      flat.push({
        type: 'title',
        id: `${sectionId}-title`,
        title: section.label,
      });

      section.items.forEach((item) => {
        flat.push({
          type: 'item',
          id: `${sectionId}-item-${item.type}:${item.name}`,
          title: item.name,
          subtitle: item.type,
          icon: getTypeIcon(item.type),
          label: item.type.toLocaleLowerCase(),
          labelColor: getTypeColor(item.type),
        });
      });

      flat.push({ type: 'divider', id: `${sectionId}-divider` });
    });

    return flat;
  });

  function close() {
    isOpen.value = false;
    term.value = '';
    results.value = [];
    totalCount.value = 0;
  }

  function navigateToResult(item: SearchResultItem) {
    if (item.type === 'TOPIC') {
      const [groupId, topicId] = item.name.split('.');
      if (groupId && topicId) {
        router.push({
          name: 'topic',
          params: { groupId, topicName: item.name },
        });
      }
    } else if (item.type === 'SUBSCRIPTION') {
      const [groupId, topicId, subscriptionId] = item.name.split('.');
      if (groupId && topicId && subscriptionId) {
        router.push({
          name: 'subscription',
          params: { groupId, topicId: `${groupId}.${topicId}`, subscriptionId },
        });
      }
    }
    // For unknown types we currently do nothing; can be extended later
    close();
  }

  watch(term, (newValue, _oldValue, onCleanup) => {
    if (!newValue) {
      results.value = [];
      totalCount.value = 0;
      return;
    }

    const handle = setTimeout(async () => {
      loading.value = true;
      try {
        const response = await search(newValue);
        const data: SearchResults = response.data;
        results.value = data.results;
        totalCount.value = data.totalCount;
      } finally {
        loading.value = false;
      }
    }, 300);

    onCleanup(() => {
      clearTimeout(handle);
    });
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
          v-model="term"
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

        <div v-if="!loading && results.length === 0 && term">
          <p class="pa-4 text-medium-emphasis">No results</p>
        </div>

        <template v-else>
          <v-list density="compact">
            <template v-for="item in items" :key="item.id">
              <v-list-subheader v-if="item.type === 'title'">
                {{ item.title }}
              </v-list-subheader>

              <v-divider v-else-if="item.type === 'divider'" />

              <command-pelette-item
                v-else-if="item.type === 'item'"
                :title="item.title"
                :subtitle="item.subtitle"
                :icon="item.icon"
                :label="item.label"
                :label-color="item.labelColor"
                @click="navigateToResult(item.raw)"
              />
            </template>
          </v-list>
        </template>
      </v-card-text>

      <v-divider />

      <v-card-actions class="justify-end">
        <span class="text-caption text-medium-emphasis mr-4">
          {{ totalCount }} results
        </span>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<style scoped lang="scss">
  .command-palette-dialog {
    align-items: flex-start;
    justify-content: center;
    padding-top: 10vh; // roughly around 1/3 screen height visually
  }

  .command-palette-card {
    width: 100%;
  }

  .command-palette-results {
    max-height: 50vh;
    overflow-y: auto;
  }
</style>
