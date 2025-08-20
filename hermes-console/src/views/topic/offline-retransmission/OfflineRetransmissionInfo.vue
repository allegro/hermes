<script setup lang="ts">
  import { isTopicOwnerOrAdmin } from '@/utils/roles-util';
  import { useAppConfigStore } from '@/store/app-config/useAppConfigStore';
  import { useI18n } from 'vue-i18n';
  import { useOfflineRetransmission } from '@/composables/topic/use-offline-retransmission/useOfflineRetransmission';
  import { useRouter } from 'vue-router';
  import OfflineRetransmissionDialog from '@/views/topic/offline-retransmission/OfflineRetransmissionDialog.vue';
  import type { OfflineRetransmissionActiveTask } from '@/api/offline-retransmission';
  import type { TopicWithSchema } from '@/api/topic';
  import type { Role } from '@/api/role';

  const { t } = useI18n();
  const router = useRouter();

  const props = defineProps<{
    topic: TopicWithSchema;
    roles: Role[] | undefined;
    tasks: Array<OfflineRetransmissionActiveTask>;
  }>();

  const TOPIC_RETRANSMISSION = 'topic';

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

  const offlineRetransmission = useOfflineRetransmission();

  const onRetransmit = async (
    targetTopic: string,
    startTimestamp: string,
    endTimestamp: string,
  ) => {
    let retransmitted = await offlineRetransmission.retransmit({
      type: TOPIC_RETRANSMISSION,
      sourceTopic: props.topic.name,
      targetTopic,
      startTimestamp,
      endTimestamp,
    });

    /*
    This is needed as we want to refresh an active offline retransmissions component
    so it fetches newest monitoring info from management.
   */
    if (retransmitted) {
      refreshPage();
    }
  };

  function refreshPage() {
    router.go(0);
  }
</script>

<template>
  <v-card>
    <template #title>
      <div class="d-flex justify-space-between">
        <p class="font-weight-bold">
          {{ t('offlineRetransmission.monitoringView.title') }}
        </p>
        <div class="d-flex justify-space-between row-gap-2">
          <v-btn
            v-if="
              configStore.loadedConfig.topic.offlineRetransmission.enabled &&
              topic.offlineStorage.enabled &&
              isTopicOwnerOrAdmin(roles)
            "
            variant="text"
            color="primary"
            prepend-icon="mdi-plus"
            data-testid="offlineRetransmissionButton"
            class="text-capitalize"
            >New retransmission task
            <OfflineRetransmissionDialog @retransmit="onRetransmit" />
          </v-btn>
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
    <template #subtitle>{{ props.tasks.length }} active task(s)</template>
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
