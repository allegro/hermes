<script setup lang="ts">
  import { groupName } from '@/utils/topic-utils/topic-utils';
  import { useI18n } from 'vue-i18n';
  import { useRouter } from 'vue-router';
  import type { Topic } from '@/api/topic';
  const { t } = useI18n();

  const router = useRouter();

  const props = defineProps<{
    topics: Topic[];
  }>();

  function onTopicClick(topic: Topic) {
    const group = groupName(topic.name);
    router.push({ path: `/ui/groups/${group}/topics/${topic.name}` });
  }
</script>

<template>
  <v-table density="comfortable" data-testid="topic-search-results" hover>
    <thead>
      <tr>
        <th>#</th>
        <th>{{ $t('search.results.topic.name') }}</th>
        <th>{{ $t('search.results.topic.owner') }}</th>
        <th></th>
      </tr>
    </thead>
    <tbody v-if="props.topics.length > 0">
      <tr
        v-for="(topic, index) in topics"
        :key="topic.name"
        class="groups-table__row"
        @click="onTopicClick(topic)"
      >
        <td class="text-medium-emphasis">
          {{ index + 1 }}
        </td>
        <td class="font-weight-medium">
          {{ topic.name }}
        </td>
        <td class="font-weight-medium">
          {{ topic.owner.id }}
        </td>
        <td>
          <v-icon icon="mdi-chevron-right"></v-icon>
        </td>
      </tr>
    </tbody>
    <tbody v-else>
      <tr>
        <th colspan="12" class="text-center text-medium-emphasis">
          {{ t('search.results.topic.noTopics') }}
        </th>
      </tr>
    </tbody>
  </v-table>
</template>
