<script setup lang="ts">
  import { useConsumerGroups } from '@/composables/consumer-groups/use-consumer-groups/useConsumerGroups';
  import { useI18n } from 'vue-i18n';
  import { useRoute } from 'vue-router';
  import ConsoleAlert from '@/components/console-alert/ConsoleAlert.vue';
  import ConsumerGroupsTable from '@/views/admin/consumer-groups/consumer-groups-table/ConsumerGroupsTable.vue';
  import LoadingSpinner from '@/components/loading-spinner/LoadingSpinner.vue';

  const { t } = useI18n();

  const route = useRoute();
  const params = route.params as Record<string, string>;
  const { subscriptionId, topicId, groupId } = params;

  const { consumerGroups, loading, error } = useConsumerGroups(
    topicId,
    subscriptionId,
  );

  const breadcrumbsItems = [
    {
      title: t('consumerGroups.breadcrumbs.home'),
      href: '/',
    },
    {
      title: t('consumerGroups.breadcrumbs.groups'),
      href: '/ui/groups',
    },
    {
      title: groupId,
      href: `/ui/groups/${groupId}`,
    },
    {
      title: topicId,
      href: `/ui/groups/${groupId}/topics/${topicId}`,
    },
    {
      title: subscriptionId,
      href: `/ui/groups/${groupId}/topics/${topicId}/subscriptions/${subscriptionId}`,
    },
    {
      title: t('consumerGroups.breadcrumbs.title'),
    },
  ];
</script>

<template>
  <v-container>
    <v-row dense>
      <v-col md="12">
        <v-breadcrumbs :items="breadcrumbsItems" density="compact" />
        <loading-spinner v-if="loading" />
        <console-alert
          v-if="error.fetchConsumerGroups"
          :title="$t('consumerGroups.connectionError.title')"
          :text="$t('consumerGroups.connectionError.text')"
          type="error"
        />
      </v-col>
    </v-row>
    <v-row dense>
      <v-col md="10">
        <p class="text-h4 font-weight-bold mb-3">
          {{ $t('consumerGroups.title') }}
        </p>
      </v-col>
    </v-row>
    <v-row dense v-if="consumerGroups">
      <v-col>
        <v-card density="compact">
          <v-col md="12">
            <p class="text-overline">{{ $t('consumerGroups.groupId') }}</p>
            <p class="text-h6 font-weight-bold mb-2">
              {{ consumerGroups[0]?.groupId }}
            </p>
          </v-col>
        </v-card>
      </v-col>
    </v-row>
    <consumer-groups-table
      v-if="consumerGroups"
      :consumer-groups="consumerGroups"
    />
  </v-container>
</template>

<style scoped lang="scss"></style>
