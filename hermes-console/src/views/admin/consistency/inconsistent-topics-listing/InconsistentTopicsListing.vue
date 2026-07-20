<script setup lang="ts">
  import { computed } from 'vue';
  import { useI18n } from 'vue-i18n';
  const { t } = useI18n();

  const props = withDefaults(
    defineProps<{
      inconsistentTopics: string[];
      filter?: string;
      selectedTopics?: string[];
      disabled?: boolean;
    }>(),
    {
      selectedTopics: () => [],
    },
  );

  const filteredTopics = computed(() => {
    return props.inconsistentTopics.filter(
      (topic) =>
        !props.filter ||
        topic.toLowerCase().includes(props.filter.toLowerCase()),
    );
  });

  const emit = defineEmits<{
    remove: [topic: string];
    'update:selectedTopics': [topics: string[]];
  }>();

  const allVisibleTopicsSelected = computed(() => {
    return (
      filteredTopics.value.length > 0 &&
      filteredTopics.value.every((topic) =>
        props.selectedTopics.includes(topic),
      )
    );
  });

  const someVisibleTopicsSelected = computed(() => {
    return (
      !allVisibleTopicsSelected.value &&
      filteredTopics.value.some((topic) => props.selectedTopics.includes(topic))
    );
  });

  function updateSelection(topic: string, selected: boolean | null) {
    const selectedTopics = new Set(props.selectedTopics);
    if (selected) {
      selectedTopics.add(topic);
    } else {
      selectedTopics.delete(topic);
    }
    emit('update:selectedTopics', [...selectedTopics]);
  }

  function updateVisibleTopicsSelection(selected: boolean | null) {
    const selectedTopics = new Set(props.selectedTopics);
    filteredTopics.value.forEach((topic) => {
      if (selected) {
        selectedTopics.add(topic);
      } else {
        selectedTopics.delete(topic);
      }
    });
    emit('update:selectedTopics', [...selectedTopics]);
  }
</script>

<template>
  <v-card class="mb-2">
    <v-table density="comfortable" hover>
      <thead>
        <tr>
          <th class="selection-column">
            <v-checkbox-btn
              data-testid="select-all-inconsistent-topics"
              :model-value="allVisibleTopicsSelected"
              :indeterminate="someVisibleTopicsSelected"
              :disabled="disabled"
              :aria-label="
                $t('consistency.inconsistentTopics.actions.selectAll')
              "
              @update:model-value="updateVisibleTopicsSelection"
            />
          </th>
          <th class="index-column">
            {{ $t('consistency.inconsistentTopics.listing.index') }}
          </th>
          <th>{{ $t('consistency.inconsistentTopics.listing.name') }}</th>
          <th></th>
        </tr>
      </thead>
      <tbody v-if="filteredTopics.length > 0">
        <tr v-for="(topic, index) in filteredTopics" :key="topic">
          <td class="selection-column">
            <v-checkbox-btn
              :model-value="selectedTopics.includes(topic)"
              :disabled="disabled"
              :aria-label="topic"
              @update:model-value="
                (selected) => updateSelection(topic, selected)
              "
            />
          </td>
          <td class="index-column text-medium-emphasis">
            {{ index + 1 }}
          </td>
          <td class="font-weight-medium">
            {{ topic }}
          </td>
          <td class="text-right">
            <v-btn
              variant="text"
              prepend-icon="mdi-delete"
              color="red"
              :disabled="disabled"
              @click="emit('remove', topic)"
            >
              {{ $t('consistency.inconsistentTopics.actions.delete') }}
            </v-btn>
          </td>
        </tr>
      </tbody>
      <tbody v-else>
        <tr>
          <th colspan="4" class="text-center text-medium-emphasis">
            {{ $t('consistency.inconsistentTopics.noTopics') }}
            <template v-if="filter">
              {{
                t('consistency.inconsistentTopics.appliedFilter', { filter })
              }}
            </template>
          </th>
        </tr>
      </tbody>
    </v-table>
  </v-card>
</template>

<style scoped lang="scss">
  .selection-column {
    width: 48px;
  }

  .index-column {
    width: 56px;
  }
</style>
