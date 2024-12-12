<script setup lang="ts">
  import { groupName } from '@/utils/topic-utils/topic-utils';
  import { useI18n } from 'vue-i18n';
  import type { InactiveTopic } from '@/api/inactive-topics';

  const { t } = useI18n();

  defineProps<{
    inactiveTopics: InactiveTopic[];
  }>();

  const formatTopics = (topics) => {
    return topics.map((topic) => {
      var jsonData = {};
      jsonData[t('inactiveTopics.listing.name')] = topic.topic;
      jsonData[t('inactiveTopics.listing.lastUsed')] = formatDateFromTimestamp(
        topic.lastPublishedTsMs,
      );
      jsonData[t('inactiveTopics.listing.lastNotified')] =
        topic.notificationTsMs.length > 0
          ? formatDateFromTimestamp(
              Math.max.apply(Math, topic.notificationTsMs),
            )
          : '';
      jsonData[t('inactiveTopics.listing.howManyTimesNotified')] =
        topic.notificationTsMs.length;
      jsonData[t('inactiveTopics.listing.whitelisted')] = topic.whitelisted;
      return jsonData;
    });
  };

  const formatDateFromTimestamp = (ts) => {
    const lastUsed = new Date(ts);
    return (
      lastUsed.getFullYear() +
      '-' +
      ('0' + (lastUsed.getMonth() + 1)).slice(-2) +
      '-' +
      ('0' + lastUsed.getDate()).slice(-2)
    );
  };

  const onTopicClick = (_, topicRepr) => {
    const qualifiedTopicName = topicRepr.item[t('inactiveTopics.listing.name')];
    window.open(
      `/ui/groups/${groupName(qualifiedTopicName)}/topics/${qualifiedTopicName}`,
      '_blank',
    );
  };
</script>

<template>
  <v-card class="mb-2">
    <v-data-table
      :items="formatTopics(inactiveTopics)"
      density="comfortable"
      hover
      @click:row="onTopicClick"
    >
    </v-data-table>
  </v-card>
</template>
