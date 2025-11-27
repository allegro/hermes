<script setup lang="ts">
  import { computed, ref, watch } from 'vue';
  import { useRouter } from 'vue-router';
  import type { SearchResultItem, SearchResults } from '@/api/SearchResults';
  import { search } from '@/api/hermes-client';

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
    }, 1000);

    onCleanup(() => {
      clearTimeout(handle);
    });
  });
</script>

<template>
  <v-dialog v-model="isOpen" max-width="640">
    <v-card>
      <v-card-title class="d-flex flex-column">
        <v-text-field
          v-model="term"
          autofocus
          variant="outlined"
          density="comfortable"
          placeholder="Search topics and subscriptions"
          prepend-inner-icon="mdi-magnify"
          hide-details
        />
        <span class="text-caption mt-2 text-medium-emphasis">
          Type to search.
        </span>
      </v-card-title>

      <v-divider />

      <v-card-text class="pa-0">
        <v-progress-linear v-if="loading" indeterminate color="primary" />

        <div v-if="!loading && results.length === 0 && term">
          <p class="pa-4 text-medium-emphasis">No results</p>
        </div>

        <v-list v-else density="compact">
          <v-list-item
            v-for="item in results"
            :key="item.type + ':' + item.name"
            @click="navigateToResult(item)"
            role="button"
          >
            <v-list-item-title>{{ item.name }}</v-list-item-title>
            <v-list-item-subtitle>{{ item.type }}</v-list-item-subtitle>
          </v-list-item>
        </v-list>
      </v-card-text>

      <v-divider />

      <v-card-actions class="justify-end">
        <span class="text-caption text-medium-emphasis mr-4">
          {{ totalCount }} results
        </span>
        <v-btn variant="text" @click="close">Close</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<style scoped lang="scss"></style>
