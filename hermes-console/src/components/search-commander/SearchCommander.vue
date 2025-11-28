<script setup lang="ts">
  import { computed, ref, watch } from 'vue';
  import { useI18n } from 'vue-i18n';
  import { useRouter } from 'vue-router';
  import { useSearch } from '@/composables/search-v2/useSearchV2';
  import CommandPalette from '@/components/command-palette/CommandPalette.vue';
  import type { CommandPaletteElement } from '@/components/command-palette/types';
  import type {
    SearchResultItem,
    SearchResultSubscriptionItem,
    SearchResultTopicItem,
  } from '@/api/SearchResults';

  const { modelValue } = defineProps<{
    modelValue: boolean;
  }>();

  const emit = defineEmits<{
    (e: 'update:modelValue', value: boolean): void;
  }>();

  const router = useRouter();
  const { t } = useI18n();

  const isOpen = computed({
    get: () => modelValue,
    set: (value: boolean) => emit('update:modelValue', value),
  });

  function close() {
    isOpen.value = false;
    term.value = '';
    totalCount.value = 0;
  }

  const term = ref('');
  const { results: apiResults, loading, runSearch } = useSearch();
  const results = ref<SearchResultItem[]>([]);
  const totalCount = ref(0);

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
      title: getSectionTitle(type),
    }));
  });

  const getSectionTitle = (type: string): string => {
    switch (type) {
      case 'TOPIC':
        return t('searchCommander.sections.topics');
      case 'SUBSCRIPTION':
        return t('searchCommander.sections.subscriptions');
      default:
        return t('searchCommander.sections.others');
    }
  };

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
    close();
  }

  const createSubscriptionItem = (
    item: SearchResultSubscriptionItem,
    sectionId: string,
  ): CommandPaletteElement => ({
    type: 'item',
    id: `${sectionId}-item-${item.type}:${item.name}`,
    title: item.name,
    subtitle: `${t('searchCommander.topic')} ${item.subscription.topicName}`,
    icon: 'mdi-rss',
    label: 'subscription',
    labelColor: 'cyan',
    onClick: () => navigateToResult(item),
  });

  const createTopicItem = (
    item: SearchResultTopicItem,
    sectionId: string,
  ): CommandPaletteElement => ({
    type: 'item',
    id: `${sectionId}-item-${item.type}:${item.name}`,
    title: item.name,
    subtitle: `${t('searchCommander.owner')} ${item.topic.owner.id}`,
    icon: 'mdi-book-open-page-variant',
    label: 'topic',
    labelColor: 'teal',
    onClick: () => navigateToResult(item),
  });

  const items = computed<CommandPaletteElement[]>(() => {
    const flat: CommandPaletteElement[] = [];

    groupedResults.value.forEach((section, index) => {
      const sectionId = `section-${section.type}`;

      flat.push({
        type: 'title',
        id: `${sectionId}-title`,
        title: section.title,
      });

      section.items.forEach((item) => {
        switch (item.type) {
          case 'TOPIC':
            flat.push(createTopicItem(item, sectionId));
            break;
          case 'SUBSCRIPTION':
            flat.push(createSubscriptionItem(item, sectionId));
            break;
        }
      });

      if (index < groupedResults.value.length - 1) {
        flat.push({ type: 'divider', id: `${sectionId}-divider` });
      }
    });

    return flat;
  });

  watch(term, (newValue, _, onCleanup) => {
    if (!newValue) {
      results.value = [];
      totalCount.value = 0;
      return;
    }

    const handle = setTimeout(async () => {
      runSearch(newValue);
    }, 300);

    onCleanup(() => {
      clearTimeout(handle);
    });
  });

  watch(apiResults, (newValue) => {
    results.value = newValue?.results || [];
    totalCount.value = newValue?.totalCount || 0;
  });
</script>

<template>
  <command-palette
    v-model="isOpen"
    v-model:search="term"
    :items="items"
    :loading="loading"
  />
</template>

<style scoped lang="scss"></style>
