<script setup lang="ts">
  import { useAppConfigStore } from '@/store/app-config/useAppConfigStore';
  import { useI18n } from 'vue-i18n';
  import type { OfflineRetransmissionActiveTask } from '@/api/offline-retransmission';

  const { t } = useI18n();

  const props = defineProps<{
    tasks: Array<OfflineRetransmissionActiveTask>;
  }>();

  const configStore = useAppConfigStore();
</script>

<template>
  <v-expansion-panels>
    <v-expansion-panel
      :title="`${t('offlineRetransmission.monitoringView.title')} (${props.tasks.length})`"
    >
      <v-expansion-panel-text>
        <v-btn
          class="mt-2"
          :href="
            configStore.loadedConfig.topic.offlineRetransmission
              .globalTaskQueueUrl
          "
          target="_blank"
        >
          {{ $t('offlineRetransmission.monitoringView.allTasksLinkTitle') }}
        </v-btn>
        <v-btn
          class="mt-2"
          :href="
            configStore.loadedConfig.topic.offlineRetransmission
              .monitoringDocsUrl
          "
          target="_blank"
        >
          {{
            $t('offlineRetransmission.monitoringView.monitoringDocsLinkTitle')
          }}
        </v-btn>
        <v-table density="comfortable" hover>
          <thead>
            <tr>
              <th>{{ $t('offlineRetransmission.monitoringView.idHeader') }}</th>
              <th>
                {{ $t('offlineRetransmission.monitoringView.typeHeader') }}
              </th>
              <th>
                {{ $t('offlineRetransmission.monitoringView.logsLinkHeader') }}
              </th>
              <th>
                {{
                  $t('offlineRetransmission.monitoringView.metricsLinkHeader')
                }}
              </th>
              <th>
                {{ $t('offlineRetransmission.monitoringView.jobLinkHeader') }}
              </th>
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
