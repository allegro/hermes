<script setup lang="ts">

const props = defineProps<{
  tasks: Array<{
    kibana: string;
    gcp: string;
  }>;
}>();
</script>

<template>
  <v-expansion-panels>
    <v-expansion-panel :title="$t('offlineRetransmission.infoView.title')">
      <v-expansion-panel-text>
        <v-btn class="mt-2">
          {{ $t('offlineRetransmission.infoView.allTasksLinkTitle') }}
        </v-btn>
        <v-table density="comfortable" hover>
          <thead>
          <tr>
            <th></th>
            <th>{{ $t('offlineRetransmission.infoView.kibanaLinkTitle') }}</th>
            <th>{{ $t('offlineRetransmission.infoView.gcpMetricsLinkTitle') }}</th>
            <th>{{ $t('offlineRetransmission.infoView.gcpJobLinkTitle') }}</th>
          </tr>
          </thead>
          <tr
              v-for="(task, index) in props.tasks"
          >
            <td class="text-medium-emphasis">
              {{ index + 1 }}
            </td>
            <td class="font-weight-medium">
              <v-btn
                  :href="task.kibana"
                  target="_blank"
                  variant="text"
                  color="blue"
              >
                Link
              </v-btn>
            </td>
            <td class="font-weight-medium">
              <v-btn
                  :href="task.gcp"
                  target="_blank"
                  variant="text"
                  color="blue"
              >
                Link
              </v-btn>
            </td>
            <td class="font-weight-medium">
              <v-btn
                  :href="task.gcp"
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