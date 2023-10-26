<script setup lang="ts">
  import { computed } from 'vue';
  import { groupName } from '@/utils/topic-utils/topic-utils';
  import { useFavorites } from '@/store/favorites/useFavorites';
  import { useI18n } from 'vue-i18n';
  import { useRouter } from 'vue-router';

  const router = useRouter();
  const { t } = useI18n();

  const favorites = useFavorites();

  const props = defineProps<{
    topics?: string[];
    filter?: string;
  }>();

  const filteredTopics = computed(() => {
    return (props.topics ?? []).filter(
      (topic) => !props.filter || topic.includes(props.filter),
    );
  });

  function onTopicClick(topicName: string) {
    router.push({
      path: `/ui/groups/${groupName(topicName)}/topics/${topicName}`,
    });
  }
</script>

<template>
  <v-card class="mb-2">
    <v-table density="comfortable" hover>
      <thead>
        <tr>
          <th>{{ t('groups.groupTopicsListing.index') }}</th>
          <th>{{ t('groups.groupTopicsListing.name') }}</th>
          <th></th>
        </tr>
      </thead>
      <tbody v-if="filteredTopics.length > 0">
        <tr
          v-for="(topic, index) in filteredTopics"
          :key="topic"
          class="topics-table__row"
        >
          <td class="text-medium-emphasis" @click="onTopicClick(topic)">
            {{ index + 1 }}
          </td>
          <td class="font-weight-medium" @click="onTopicClick(topic)">
            {{ topic }}
          </td>
          <td class="text-right" style="width: 50px">
            <v-tooltip
              :text="$t('topicView.header.actions.removeFromFavorites')"
            >
              <template v-slot:activator="{ props }">
                <v-btn
                  icon="mdi-star"
                  variant="plain"
                  v-bind="props"
                  @click="favorites.removeTopic(topic)"
                />
              </template>
            </v-tooltip>
          </td>
        </tr>
      </tbody>
      <tbody v-else>
        <tr>
          <th colspan="3" class="text-center text-medium-emphasis">
            {{ t('groups.groupTopicsListing.noTopics') }}
            <template v-if="filter">
              {{ t('groups.groupTopicsListing.appliedFilter', { filter }) }}
            </template>
          </th>
        </tr>
      </tbody>
    </v-table>
  </v-card>
</template>

<style scoped lang="scss">
  .topics-table__row:hover {
    cursor: pointer;
  }
</style>
