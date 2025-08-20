<script setup lang="ts">
  import { useAppConfigStore } from '@/store/app-config/useAppConfigStore';
  import { useI18n } from 'vue-i18n';
  import type { OfflineRetransmissionActiveTask } from '@/api/offline-retransmission';

  const { t } = useI18n();

  const props = defineProps<{
    tasks: Array<OfflineRetransmissionActiveTask>;
  }>();

  const taskTableHeaders = [
    {
      title: t('offlineRetransmission.monitoringView.idHeader'),
      key: 'taskId',
    },
    {
      title: t('offlineRetransmission.monitoringView.typeHeader'),
      key: 'type',
    },
    {
      title: t('offlineRetransmission.monitoringView.logsLinkHeader'),
      key: 'logsUrl',
      sortable: false,
    },
    {
      title: t('offlineRetransmission.monitoringView.metricsLinkHeader'),
      key: 'metricsUrl',
      sortable: false,
    },
    {
      title: t('offlineRetransmission.monitoringView.jobLinkHeader'),
      key: 'jobDetailsUrl',
      sortable: false,
    },
  ];

  const configStore = useAppConfigStore();
</script>

<template>
  <v-card>
    <template #title>
      <div class="d-flex justify-space-between">
        <p class="font-weight-bold">
          {{ t('offlineRetransmission.monitoringView.title') }} ({{
            props.tasks.length
          }})
        </p>
        <div class="d-flex justify-space-between row-gap-2">
          <v-btn
            class="text-capitalize"
            prepend-icon="mdi-open-in-new"
            :href="
              configStore.loadedConfig.topic.offlineRetransmission
                .globalTaskQueueUrl
            "
            target="_blank"
            variant="text"
            color="primary"
          >
            {{ $t('offlineRetransmission.monitoringView.allTasksLinkTitle') }}
          </v-btn>
          <v-btn
            class="text-capitalize"
            prepend-icon="mdi-open-in-new"
            :href="
              configStore.loadedConfig.topic.offlineRetransmission
                .monitoringDocsUrl
            "
            target="_blank"
            variant="text"
            color="primary"
          >
            {{
              $t('offlineRetransmission.monitoringView.monitoringDocsLinkTitle')
            }}
          </v-btn>
        </div>
      </div>
    </template>
    <v-card-text>
      <v-data-table
        :items="props.tasks"
        :headers="taskTableHeaders"
        items-per-page="-1"
      >
        <template v-slot:[`item.logsUrl`]="{ item }">
          <a
            :href="item.logsUrl"
            target="_blank"
            class="text-decoration-none text-button text-none text-primary"
            >View logs</a
          >
          <v-icon color="primary" size="x-small" class="ml-1"
            >mdi-open-in-new</v-icon
          >
        </template>
        <template v-slot:[`item.metricsUrl`]="{ item }">
          <a
            :href="item.metricsUrl"
            target="_blank"
            class="text-decoration-none text-button text-none text-primary"
            >View metrics</a
          >
          <v-icon color="primary" size="x-small" class="ml-1"
            >mdi-open-in-new</v-icon
          >
        </template>
        <template v-slot:[`item.jobDetailsUrl`]="{ item }">
          <a
            :href="item.jobDetailsUrl"
            target="_blank"
            class="text-decoration-none text-button text-none text-primary"
            >View details</a
          >
          <v-icon color="primary" size="x-small" class="ml-1"
            >mdi-open-in-new</v-icon
          >
        </template>
      </v-data-table>
    </v-card-text>
  </v-card>
</template>

<style scoped lang="scss"></style>
