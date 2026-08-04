<script setup lang="ts">
  import { computed, ref } from 'vue';
  import { storeToRefs } from 'pinia';
  import { topicKey } from '@/api/kafka-inconsistency';
  import { useDialog } from '@/composables/dialog/use-dialog/useDialog';
  import { useI18n } from 'vue-i18n';
  import { useKafkaConfigConsistency } from '@/composables/kafka-config-consistency/use-kafka-config-consistency/useKafkaConfigConsistency';
  import { useKafkaConfigConsistencyStore } from '@/store/consistency/useKafkaConfigConsistencyStore';
  import ConfirmationDialog from '@/components/confirmation-dialog/ConfirmationDialog.vue';
  import ConsoleAlert from '@/components/console-alert/ConsoleAlert.vue';
  import LoadingSpinner from '@/components/loading-spinner/LoadingSpinner.vue';
  import type { InconsistentKafkaTopic } from '@/api/kafka-inconsistency';

  const { t } = useI18n();
  const store = useKafkaConfigConsistencyStore();
  const { clusters, selectedCluster, lastDryRun } = storeToRefs(store);
  const {
    inconsistencies,
    loading,
    error,
    batchProgress,
    batchTotal,
    batchResult,
    fetchInconsistencies,
    reviewSync,
    applyReviewedSync,
    reviewBootstrap,
    applyReviewedBootstrap,
  } = useKafkaConfigConsistency();

  const selectedTopicKeys = ref<string[]>([]);
  const expandedTopicKeys = ref<string[]>([]);
  const dryRun = ref(true);
  const reviewing = ref(false);
  const applying = ref(false);

  const clusterItems = computed(() => [
    { title: t('consistency.kafkaConfig.cluster.all'), value: null },
    ...clusters.value.map((cluster) => ({ title: cluster, value: cluster })),
  ]);
  const selectableTopics = computed(() =>
    inconsistencies.value.filter((topic) => topic.existsOnBroker),
  );
  const selectedTopics = computed(() => {
    const keys = new Set(selectedTopicKeys.value);
    return inconsistencies.value.filter((topic) => keys.has(topicKey(topic)));
  });
  const allTopicsSelected = computed(
    () =>
      selectableTopics.value.length > 0 &&
      selectableTopics.value.every((topic) =>
        selectedTopicKeys.value.includes(topicKey(topic)),
      ),
  );
  const someTopicsSelected = computed(
    () => !allTopicsSelected.value && selectedTopicKeys.value.length > 0,
  );
  const syncDryRun = computed(() =>
    lastDryRun.value?.operation === 'sync' ? lastDryRun.value : null,
  );
  const bootstrapDryRun = computed(() =>
    lastDryRun.value?.operation === 'bootstrap' ? lastDryRun.value : null,
  );
  const displayedInconsistencies = computed(
    () => syncDryRun.value?.inconsistencies ?? inconsistencies.value,
  );
  const busy = computed(
    () =>
      loading.value ||
      reviewing.value ||
      applying.value ||
      batchProgress.value !== undefined,
  );

  const {
    isDialogOpened: isBootstrapDialogOpened,
    actionButtonEnabled: bootstrapActionButtonEnabled,
    openDialog: openBootstrapDialog,
    closeDialog: closeBootstrapDialog,
    enableActionButton: enableBootstrapActionButton,
    disableActionButton: disableBootstrapActionButton,
  } = useDialog();

  async function onClusterChange(clusterName: string | null) {
    store.selectCluster(clusterName);
    selectedTopicKeys.value = [];
    expandedTopicKeys.value = [];
    await fetchInconsistencies(clusterName || undefined);
  }

  function updateSelection(
    topic: InconsistentKafkaTopic,
    selected: boolean | null,
  ) {
    const keys = new Set(selectedTopicKeys.value);
    if (selected) keys.add(topicKey(topic));
    else keys.delete(topicKey(topic));
    selectedTopicKeys.value = [...keys];
  }

  function updateAllSelection(selected: boolean | null) {
    selectedTopicKeys.value = selected
      ? selectableTopics.value.map(topicKey)
      : [];
  }

  function toggleExpanded(topic: InconsistentKafkaTopic) {
    const key = topicKey(topic);
    const keys = new Set(expandedTopicKeys.value);
    if (keys.has(key)) keys.delete(key);
    else keys.add(key);
    expandedTopicKeys.value = [...keys];
  }

  async function runReview(topics?: InconsistentKafkaTopic[]) {
    reviewing.value = true;
    const reviewed = await reviewSync(topics);
    reviewing.value = false;
    if (reviewed) dryRun.value = true;
  }

  async function applySync() {
    applying.value = true;
    await applyReviewedSync();
    applying.value = false;
    selectedTopicKeys.value = [];
    dryRun.value = true;
  }

  async function runBootstrapReview() {
    if (!selectedCluster.value) return;
    reviewing.value = true;
    await reviewBootstrap(selectedCluster.value);
    reviewing.value = false;
  }

  async function applyBootstrap() {
    disableBootstrapActionButton();
    applying.value = true;
    const applied = await applyReviewedBootstrap();
    applying.value = false;
    enableBootstrapActionButton();
    if (applied) closeBootstrapDialog();
  }
</script>

<template>
  <confirmation-dialog
    v-model="isBootstrapDialogOpened"
    :actionButtonEnabled="bootstrapActionButtonEnabled"
    :title="$t('consistency.kafkaConfig.bootstrap.confirmation.title')"
    :text="
      t('consistency.kafkaConfig.bootstrap.confirmation.text', {
        cluster: bootstrapDryRun?.clusterName,
        count: bootstrapDryRun?.topicNames.length,
      })
    "
    @action="applyBootstrap"
    @cancel="closeBootstrapDialog"
  />

  <v-card class="mb-2 pa-4">
    <v-row align="center">
      <v-col cols="12" md="4">
        <v-select
          :model-value="selectedCluster"
          :items="clusterItems"
          :label="$t('consistency.kafkaConfig.cluster.label')"
          :disabled="busy"
          hide-details
          @update:model-value="onClusterChange"
        />
      </v-col>
      <v-col cols="12" md="3">
        <v-switch
          v-model="dryRun"
          :label="$t('consistency.kafkaConfig.dryRun')"
          :disabled="
            !syncDryRun || syncDryRun.inconsistencies.length === 0 || busy
          "
          color="primary"
          hide-details
        />
      </v-col>
      <v-col cols="12" md="5" class="d-flex flex-wrap justify-end ga-2">
        <v-btn
          :disabled="selectedTopics.length === 0 || busy || !dryRun"
          @click="runReview(selectedTopics)"
        >
          {{ $t('consistency.kafkaConfig.actions.syncSelected') }}
        </v-btn>
        <v-btn
          :disabled="inconsistencies.length === 0 || busy || !dryRun"
          @click="runReview()"
        >
          {{ $t('consistency.kafkaConfig.actions.syncAll') }}
        </v-btn>
        <v-btn
          color="primary"
          :disabled="
            dryRun ||
            !syncDryRun ||
            syncDryRun.inconsistencies.length === 0 ||
            busy
          "
          @click="applySync"
        >
          {{ $t('consistency.kafkaConfig.actions.apply') }}
        </v-btn>
      </v-col>
    </v-row>

    <v-row align="center" class="mt-2">
      <v-col cols="12" md="8">
        <v-alert type="warning" variant="tonal" density="compact">
          {{ $t('consistency.kafkaConfig.bootstrap.warning') }}
        </v-alert>
      </v-col>
      <v-col cols="12" md="4" class="d-flex justify-end ga-2">
        <v-btn :disabled="!selectedCluster || busy" @click="runBootstrapReview">
          {{ $t('consistency.kafkaConfig.bootstrap.review') }}
        </v-btn>
        <v-btn
          color="warning"
          :disabled="
            !bootstrapDryRun || bootstrapDryRun.topicNames.length === 0 || busy
          "
          @click="openBootstrapDialog"
        >
          {{ $t('consistency.kafkaConfig.bootstrap.apply') }}
        </v-btn>
      </v-col>
    </v-row>
  </v-card>

  <loading-spinner v-if="loading || reviewing" />
  <console-alert
    v-if="error.fetch"
    :title="$t('consistency.connectionError.title')"
    :text="$t('consistency.connectionError.text')"
    type="error"
  />
  <console-alert
    v-if="error.action"
    :title="$t('consistency.kafkaConfig.actionError.title')"
    :text="$t('consistency.kafkaConfig.actionError.text')"
    type="error"
  />

  <v-alert
    v-if="syncDryRun"
    class="mb-2"
    type="info"
    variant="tonal"
    data-testid="kafka-config-sync-review"
  >
    {{
      t('consistency.kafkaConfig.review.sync', {
        count: syncDryRun.inconsistencies.length,
      })
    }}
  </v-alert>
  <v-alert
    v-if="bootstrapDryRun"
    class="mb-2"
    type="info"
    variant="tonal"
    data-testid="kafka-config-bootstrap-review"
  >
    {{
      t('consistency.kafkaConfig.review.bootstrap', {
        cluster: bootstrapDryRun.clusterName,
        count: bootstrapDryRun.topicNames.length,
      })
    }}
  </v-alert>
  <v-alert
    v-if="batchProgress !== undefined"
    class="mb-2"
    type="info"
    variant="tonal"
    data-testid="kafka-config-sync-progress"
  >
    {{
      t('consistency.kafkaConfig.progress', {
        completed: batchProgress,
        total: batchTotal,
      })
    }}
  </v-alert>
  <v-alert
    v-if="batchResult"
    class="mb-2"
    :type="batchResult.failed === 0 ? 'success' : 'warning'"
    variant="tonal"
  >
    {{ t('consistency.kafkaConfig.complete', batchResult) }}
  </v-alert>

  <v-card class="mb-2">
    <v-table density="comfortable" hover>
      <thead>
        <tr>
          <th class="selection-column">
            <v-checkbox-btn
              :model-value="allTopicsSelected"
              :indeterminate="someTopicsSelected"
              :disabled="busy"
              :aria-label="$t('consistency.kafkaConfig.actions.selectAll')"
              data-testid="select-all-kafka-config-topics"
              @update:model-value="updateAllSelection"
            />
          </th>
          <th>{{ $t('consistency.kafkaConfig.listing.topic') }}</th>
          <th>{{ $t('consistency.kafkaConfig.listing.kafkaTopic') }}</th>
          <th>{{ $t('consistency.kafkaConfig.listing.cluster') }}</th>
          <th>{{ $t('consistency.kafkaConfig.listing.status') }}</th>
          <th></th>
        </tr>
      </thead>
      <tbody v-if="displayedInconsistencies.length > 0">
        <template
          v-for="topic in displayedInconsistencies"
          :key="topicKey(topic)"
        >
          <tr>
            <td class="selection-column">
              <v-checkbox-btn
                :model-value="selectedTopicKeys.includes(topicKey(topic))"
                :disabled="busy || !topic.existsOnBroker"
                :aria-label="topicKey(topic)"
                @update:model-value="
                  (selected) => updateSelection(topic, selected)
                "
              />
            </td>
            <td class="font-weight-medium">{{ topic.qualifiedTopicName }}</td>
            <td>{{ topic.kafkaTopicName }}</td>
            <td>{{ topic.clusterName }}</td>
            <td>
              <v-chip
                :color="topic.existsOnBroker ? 'warning' : 'error'"
                size="small"
              >
                {{
                  $t(
                    `consistency.kafkaConfig.status.${
                      topic.existsOnBroker ? 'present' : 'missing'
                    }`,
                  )
                }}
              </v-chip>
            </td>
            <td class="text-right text-no-wrap">
              <v-btn
                variant="text"
                :disabled="topic.configDiffs.length === 0"
                @click="toggleExpanded(topic)"
              >
                {{ $t('consistency.kafkaConfig.actions.diffs') }}
              </v-btn>
              <v-btn
                variant="text"
                color="primary"
                :disabled="busy || !dryRun || !topic.existsOnBroker"
                @click="runReview([topic])"
              >
                {{ $t('consistency.kafkaConfig.actions.sync') }}
              </v-btn>
            </td>
          </tr>
          <tr v-if="expandedTopicKeys.includes(topicKey(topic))">
            <td colspan="6" class="pa-4 bg-grey-lighten-5">
              <v-table density="compact">
                <thead>
                  <tr>
                    <th>{{ $t('consistency.kafkaConfig.diffs.key') }}</th>
                    <th>{{ $t('consistency.kafkaConfig.diffs.expected') }}</th>
                    <th>{{ $t('consistency.kafkaConfig.diffs.actual') }}</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="diff in topic.configDiffs" :key="diff.key">
                    <td>{{ diff.key }}</td>
                    <td>
                      {{
                        diff.expected ??
                        $t('consistency.kafkaConfig.diffs.unset')
                      }}
                    </td>
                    <td>
                      {{
                        diff.actual ?? $t('consistency.kafkaConfig.diffs.unset')
                      }}
                    </td>
                  </tr>
                </tbody>
              </v-table>
            </td>
          </tr>
        </template>
      </tbody>
      <tbody v-else-if="!loading">
        <tr>
          <th colspan="6" class="text-center text-medium-emphasis">
            {{ $t('consistency.kafkaConfig.noTopics') }}
          </th>
        </tr>
      </tbody>
    </v-table>
  </v-card>
</template>

<style scoped lang="scss">
  .selection-column {
    width: 48px;
  }
</style>
