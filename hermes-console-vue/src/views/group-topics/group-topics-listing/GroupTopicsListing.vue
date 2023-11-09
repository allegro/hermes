<script setup lang="ts">
  import { computed } from 'vue';
  import { useI18n } from 'vue-i18n';
  import { useRouter } from 'vue-router';
  import type { Group } from '@/composables/groups/use-groups/useGroups';

  const { t } = useI18n();
  const router = useRouter();

  const props = defineProps<{
    group?: Group;
    filter?: string;
  }>();

  const filteredTopics = computed(() => {
    return (props.group?.topics ?? []).filter(
      (topic) =>
        !props.filter ||
        topic.toLowerCase().includes(props.filter.toLowerCase()),
    );
  });

  function onTopicClick(topicName: string) {
    router.push({
      path: `/ui/groups/${props.group?.name}/topics/${topicName}`,
    });
  }

  function onTopicBlankClick(topicName: string) {
    console.log(router.currentRoute.value.path);
    window.open(
      `${router.currentRoute.value.path}/topics/${topicName}`,
      '_blank',
    );
  }
</script>

<template>
  <v-card class="mb-2">
    <v-table density="comfortable" hover>
      <thead>
        <tr>
          <th>{{ t('groups.groupTopicsListing.index') }}</th>
          <th>{{ t('groups.groupTopicsListing.name') }}</th>
        </tr>
      </thead>
      <tbody v-if="filteredTopics.length > 0">
        <tr
          v-for="(topic, index) in filteredTopics"
          :key="topic"
          class="topics-table__row"
          @click.exact="onTopicClick(topic)"
          @click.meta="onTopicBlankClick(topic)"
          @click.ctrl="onTopicBlankClick(topic)"
          @contextmenu="onTopicBlankClick(topic)"
        >
          <td class="text-medium-emphasis">
            {{ index + 1 }}
          </td>
          <td class="font-weight-medium">
            {{ topic }}
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
