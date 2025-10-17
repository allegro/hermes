<script setup lang="ts">
  import { isTopicOwnerOrAdmin } from '@/utils/roles-util';
  import { useAppConfigStore } from '@/store/app-config/useAppConfigStore';
  import { useI18n } from 'vue-i18n';
  import { useOfflineRetransmission } from '@/composables/topic/use-offline-retransmission/useOfflineRetransmission';
  import { useRouter } from 'vue-router';
  import OfflineRetransmissionDialog from '@/views/topic/offline-retransmission/OfflineRetransmissionDialog.vue';
  import SimpleLink from '@/components/link/SimpleLink.vue';
  import type { OfflineRetransmissionActiveTask } from '@/api/offline-retransmission';
  import type { Role } from '@/api/role';
  import type { TopicWithSchema } from '@/api/topic';

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
    <v-card-item class="border-b">
      <div class="d-flex justify-space-between">
        <div>
          <v-card-title class="font-weight-bold">
            {{ t('offlineRetransmission.monitoringView.title') }}
          </v-card-title>
          <v-card-subtitle
            >{{ props.tasks.length }} active task(s)
          </v-card-subtitle>
        </div>

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
    </v-card-item>

    <v-card-text>
      <v-data-table
        :items="props.tasks"
        :headers="taskTableHeaders"
        items-per-page="-1"
      >
        <template v-slot:[`item.logsUrl`]="{ item }">
          <simple-link :href="item.logsUrl" text="View logs" open-in-new-tab />
        </template>
        <template v-slot:[`item.metricsUrl`]="{ item }">
          <simple-link
            :href="item.metricsUrl"
            text="View metrics"
            open-in-new-tab
          />
        </template>
        <template v-slot:[`item.jobDetailsUrl`]="{ item }">
          <simple-link
            :href="item.jobDetailsUrl"
            text="View details"
            open-in-new-tab
          />
        </template>
        <template v-slot:[`item.type`]="{ item }">
          <v-chip size="small" color="accent">{{ item.type }} </v-chip>
        </template>
      </v-data-table>
    </v-card-text>
  </v-card>
</template>

<style scoped lang="scss"></style>
