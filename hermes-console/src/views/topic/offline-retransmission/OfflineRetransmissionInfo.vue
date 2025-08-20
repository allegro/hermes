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
              {{ $t('offlineRetransmission.monitoringView.metricsLinkHeader') }}
            </th>
            <th>
              {{ $t('offlineRetransmission.monitoringView.jobLinkHeader') }}
            </th>
          </tr>
        </thead>
        <tbody>
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
        </tbody>
      </v-table>
    </v-card-text>
  </v-card>
</template>

<style scoped lang="scss"></style>
