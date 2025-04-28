<script setup lang="ts">
  import { useAppConfigStore } from '@/store/app-config/useAppConfigStore';
  import { useI18n } from 'vue-i18n';

  const { t } = useI18n();

  const props = defineProps<{
    tasks: Array<{
      taskId: string;
      type: string;
      logsUrl: string;
      metricsUrl: string;
      jobDetailsUrl: string;
    }>;
  }>();

  const configStore = useAppConfigStore();
</script>

<template>
  <v-expansion-panels>
    <v-expansion-panel
      :title="`${t('offlineRetransmission.infoView.title')} (${props.tasks.length})`"
    >
      <v-expansion-panel-text>
        <v-btn
          class="mt-2"
          :href="
            configStore.loadedConfig.topic
              .offlineRetransmissionGlobalTaskQueueUrl
          "
          target="_blank"
        >
          {{ $t('offlineRetransmission.infoView.allTasksLinkTitle') }}
        </v-btn>
        <v-btn
          class="mt-2"
          :href="
            configStore.loadedConfig.topic.offlineRetransmissionMonitoringUrl
          "
          target="_blank"
        >
          {{ $t('offlineRetransmission.infoView.monitoringLinkTitle') }}
        </v-btn>
        <v-table density="comfortable" hover>
          <thead>
            <tr>
              <th>{{ $t('offlineRetransmission.infoView.idHeader') }}</th>
              <th>{{ $t('offlineRetransmission.infoView.typeHeader') }}</th>
              <th>{{ $t('offlineRetransmission.infoView.logsLinkHeader') }}</th>
              <th>
                {{ $t('offlineRetransmission.infoView.metricsLinkHeader') }}
              </th>
              <th>{{ $t('offlineRetransmission.infoView.monitoring') }}</th>
              <th>{{ $t('offlineRetransmission.infoView.jobLinkHeader') }}</th>
            </tr>
          </thead>
          <tr v-for="task in props.tasks" v-bind:key="task.taskId">
            <td class="text-medium-emphasis">
              {{ task.taskId }}
            </td>
            <td class="text-medium-emphasis">
              {{ task.type }}
            </td>
            <td class="font-weight-medium">
              <v-btn
                :href="task.logsUrl"
                target="_blank"
                variant="text"
                color="blue"
              >
                Link
              </v-btn>
            </td>
            <td class="font-weight-medium">
              <v-btn
                :href="task.metricsUrl"
                target="_blank"
                variant="text"
                color="blue"
              >
                Link
              </v-btn>
            </td>
            <td class="font-weight-medium">
              <v-btn
                :href="task.jobDetailsUrl"
                target="_blank"
                variant="text"
                color="blue"
              >
                Link
              </v-btn>
            </td>
          </tr>
        </v-table>
      </v-expansion-panel-text>
    </v-expansion-panel>
  </v-expansion-panels>
</template>

<style scoped lang="scss"></style>
