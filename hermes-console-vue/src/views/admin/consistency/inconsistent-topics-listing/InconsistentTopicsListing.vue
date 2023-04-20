<script setup lang="ts">
  import { computed } from 'vue';
  import { useI18n } from 'vue-i18n';
  const { t } = useI18n();

  const props = defineProps<{
    inconsistentTopics: string[];
    filter?: string;
  }>();

  const filteredTopics = computed(() => {
    return props.inconsistentTopics.filter(
      (topic) => !props.filter || topic.includes(props.filter),
    );
  });
</script>

<template>
  <v-card class="mb-2">
    <v-table density="comfortable" hover>
      <thead>
        <tr>
          <th>{{ t('consistency.inconsistentTopics.listing.index') }}</th>
          <th>{{ t('consistency.inconsistentTopics.listing.name') }}</th>
          <th></th>
        </tr>
      </thead>
      <tbody v-if="filteredTopics.length > 0">
        <tr v-for="(topic, index) in filteredTopics" :key="topic">
          <td class="text-medium-emphasis">
            {{ index + 1 }}
          </td>
          <td class="font-weight-medium">
            {{ topic }}
          </td>
          <td class="text-right">
            <v-btn variant="text" prepend-icon="mdi-delete" color="red">
              {{ t('consistency.inconsistentTopics.actions.delete') }}
            </v-btn>
          </td>
        </tr>
      </tbody>
      <tbody v-else>
        <tr>
          <th colspan="3" class="text-center text-medium-emphasis">
            {{ t('consistency.inconsistentTopics.noTopics') }}
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

<style scoped lang="scss"></style>
