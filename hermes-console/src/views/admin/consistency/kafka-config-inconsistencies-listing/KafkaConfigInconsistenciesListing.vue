<script setup lang="ts">
  import { computed, ref, watch } from 'vue';
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
  const search = ref('');
  const page = ref(1);
  const reviewing = ref(false);
  const applying = ref(false);
  const pendingSyncCount = ref(0);
  const pendingSyncScope = ref('');
  const pendingBootstrapCount = ref(0);
  const pendingBootstrapCluster = ref('');
  const itemsPerPage = 50;

  store.clearDryRun();

  const clusterItems = computed(() => [
    { title: t('consistency.kafkaConfig.cluster.all'), value: null },
    ...clusters.value.map((cluster) => ({ title: cluster, value: cluster })),
  ]);
  const selectedTopicKeySet = computed(() => new Set(selectedTopicKeys.value));
  const selectedTopics = computed(() => {
    return inconsistencies.value.filter((topic) =>
      selectedTopicKeySet.value.has(topicKey(topic)),
    );
  });
  const allPageTopicsSelected = computed(
    () =>
      selectablePageTopics.value.length > 0 &&
      selectablePageTopics.value.every((topic) =>
        selectedTopicKeySet.value.has(topicKey(topic)),
      ),
  );
  const somePageTopicsSelected = computed(
    () =>
      !allPageTopicsSelected.value &&
      selectablePageTopics.value.some((topic) =>
        selectedTopicKeySet.value.has(topicKey(topic)),
      ),
  );
  const syncDryRun = computed(() =>
    lastDryRun.value?.operation === 'sync' ? lastDryRun.value : null,
  );
  const bootstrapDryRun = computed(() =>
    lastDryRun.value?.operation === 'bootstrap' ? lastDryRun.value : null,
  );
  const displayedInconsistencies = computed(() => inconsistencies.value);
  const filteredInconsistencies = computed(() => {
    const query = search.value.trim().toLowerCase();
    if (!query) return displayedInconsistencies.value;
    return displayedInconsistencies.value.filter(
      (topic) =>
        topic.qualifiedTopicName.toLowerCase().includes(query) ||
        topic.kafkaTopicName.toLowerCase().includes(query) ||
        topic.clusterName.toLowerCase().includes(query),
    );
  });
  const pageCount = computed(() =>
    Math.max(1, Math.ceil(filteredInconsistencies.value.length / itemsPerPage)),
  );
  const paginatedInconsistencies = computed(() => {
    const start = (page.value - 1) * itemsPerPage;
    return filteredInconsistencies.value.slice(start, start + itemsPerPage);
  });
  const selectablePageTopics = computed(() =>
    paginatedInconsistencies.value.filter((topic) => topic.existsOnBroker),
  );
  const firstVisibleItem = computed(() =>
    filteredInconsistencies.value.length === 0
      ? 0
      : (page.value - 1) * itemsPerPage + 1,
  );
  const lastVisibleItem = computed(() =>
    Math.min(page.value * itemsPerPage, filteredInconsistencies.value.length),
  );
  const totalDriftedCount = computed(
    () => inconsistencies.value.filter((topic) => topic.existsOnBroker).length,
  );
  const totalMissingCount = computed(
    () => inconsistencies.value.length - totalDriftedCount.value,
  );
  const displayedDriftedCount = computed(
    () =>
      displayedInconsistencies.value.filter((topic) => topic.existsOnBroker)
        .length,
  );
  const displayedMissingCount = computed(
    () => displayedInconsistencies.value.length - displayedDriftedCount.value,
  );
  const bootstrapDisabledReason = computed(() => {
    if (!selectedCluster.value) {
      return t('consistency.kafkaConfig.bootstrap.selectCluster');
    }
    if (totalMissingCount.value === 0) {
      return t('consistency.kafkaConfig.bootstrap.noneMissing');
    }
    return t('consistency.kafkaConfig.bootstrap.missingCount', {
      count: totalMissingCount.value,
    });
  });
  const busy = computed(
    () =>
      loading.value ||
      reviewing.value ||
      applying.value ||
      batchProgress.value !== undefined,
  );

  const {
    isDialogOpened: isSyncDialogOpened,
    actionButtonEnabled: syncActionButtonEnabled,
    openDialog: openSyncDialog,
    closeDialog: closeSyncDialog,
    enableActionButton: enableSyncActionButton,
    disableActionButton: disableSyncActionButton,
  } = useDialog();

  const {
    isDialogOpened: isBootstrapDialogOpened,
    actionButtonEnabled: bootstrapActionButtonEnabled,
    openDialog: openBootstrapDialog,
    closeDialog: closeBootstrapDialog,
    enableActionButton: enableBootstrapActionButton,
    disableActionButton: disableBootstrapActionButton,
  } = useDialog();

  watch(pageCount, (count) => {
    if (page.value > count) page.value = count;
  });

  async function onClusterChange(clusterName: string | null) {
    store.selectCluster(clusterName);
    selectedTopicKeys.value = [];
    expandedTopicKeys.value = [];
    page.value = 1;
    await fetchInconsistencies(clusterName || undefined);
  }

  function onSearchChange(value: string | null) {
    search.value = value ?? '';
    page.value = 1;
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
    const keys = new Set(selectedTopicKeys.value);
    selectablePageTopics.value.forEach((topic) => {
      if (selected) keys.add(topicKey(topic));
      else keys.delete(topicKey(topic));
    });
    selectedTopicKeys.value = [...keys];
  }

  function toggleExpanded(topic: InconsistentKafkaTopic) {
    const key = topicKey(topic);
    const keys = new Set(expandedTopicKeys.value);
    if (keys.has(key)) keys.delete(key);
    else keys.add(key);
    expandedTopicKeys.value = [...keys];
  }

  async function prepareSync(topics?: InconsistentKafkaTopic[]) {
    reviewing.value = true;
    try {
      const reviewed = await reviewSync(topics);
      if (reviewed) {
        pendingSyncCount.value = syncDryRun.value?.inconsistencies.length ?? 0;
        pendingSyncScope.value =
          selectedCluster.value ?? t('consistency.kafkaConfig.cluster.all');
        page.value = 1;
        openSyncDialog();
      }
    } finally {
      reviewing.value = false;
    }
  }

  async function confirmSync() {
    disableSyncActionButton();
    applying.value = true;
    try {
      const result = await applyReviewedSync();
      if (result.failed > 0)
        error.value.action = new Error('Some topics failed to synchronize');
      selectedTopicKeys.value = [];
      page.value = 1;
    } finally {
      applying.value = false;
      enableSyncActionButton();
      closeSyncDialog();
      pendingSyncCount.value = 0;
      pendingSyncScope.value = '';
    }
  }

  function cancelSync() {
    store.clearDryRun();
    pendingSyncCount.value = 0;
    pendingSyncScope.value = '';
    closeSyncDialog();
  }

  async function prepareBootstrap(clusterName?: string) {
    const targetCluster = clusterName ?? selectedCluster.value;
    if (!targetCluster) return;
    if (selectedCluster.value !== targetCluster) {
      store.selectCluster(targetCluster);
      selectedTopicKeys.value = [];
      page.value = 1;
    }
    reviewing.value = true;
    try {
      const reviewed = await reviewBootstrap(targetCluster);
      if (reviewed && bootstrapDryRun.value?.topicNames.length) {
        pendingBootstrapCount.value = bootstrapDryRun.value.topicNames.length;
        pendingBootstrapCluster.value = targetCluster;
        openBootstrapDialog();
      }
    } finally {
      reviewing.value = false;
    }
  }

  async function applyBootstrap() {
    disableBootstrapActionButton();
    applying.value = true;
    try {
      const applied = await applyReviewedBootstrap();
      if (!applied)
        error.value.action = new Error('Creating missing topics failed');
      page.value = 1;
    } finally {
      applying.value = false;
      enableBootstrapActionButton();
      closeBootstrapDialog();
      pendingBootstrapCount.value = 0;
      pendingBootstrapCluster.value = '';
    }
  }

  function cancelBootstrap() {
    store.clearDryRun();
    pendingBootstrapCount.value = 0;
    pendingBootstrapCluster.value = '';
    closeBootstrapDialog();
  }

  function clearSelection() {
    selectedTopicKeys.value = [];
  }
</script>

<template>
  <confirmation-dialog
    v-model="isSyncDialogOpened"
    :actionButtonEnabled="syncActionButtonEnabled"
    action-color="primary"
    icon="mdi-sync"
    :title="$t('consistency.kafkaConfig.sync.confirmation.title')"
    :text="
      t('consistency.kafkaConfig.sync.confirmation.text', {
        count: pendingSyncCount,
        scope: pendingSyncScope,
      })
    "
    :action-text="$t('consistency.kafkaConfig.sync.confirmation.action')"
    @action="confirmSync"
    @cancel="cancelSync"
  />
  <confirmation-dialog
    v-model="isBootstrapDialogOpened"
    :actionButtonEnabled="bootstrapActionButtonEnabled"
    action-color="warning"
    icon="mdi-database-plus"
    :title="$t('consistency.kafkaConfig.bootstrap.confirmation.title')"
    :text="
      t('consistency.kafkaConfig.bootstrap.confirmation.text', {
        cluster: pendingBootstrapCluster,
        count: pendingBootstrapCount,
      })
    "
    :action-text="$t('consistency.kafkaConfig.bootstrap.confirmation.action')"
    @action="applyBootstrap"
    @cancel="cancelBootstrap"
  />

  <v-card class="mb-4 control-card" variant="outlined">
    <v-card-text>
      <v-row align="start" class="ga-lg-4">
        <v-col cols="12" lg="4">
          <div class="text-overline text-medium-emphasis mb-1">
            {{ $t('consistency.kafkaConfig.scope') }}
          </div>
          <v-select
            :model-value="selectedCluster"
            :items="clusterItems"
            :label="$t('consistency.kafkaConfig.cluster.label')"
            :disabled="busy"
            density="compact"
            hide-details
            variant="outlined"
            @update:model-value="onClusterChange"
          />
        </v-col>

        <v-col cols="12" lg="8">
          <div class="text-overline text-medium-emphasis mb-1">
            {{ $t('consistency.kafkaConfig.sync.heading') }}
          </div>
          <div class="d-flex flex-wrap align-center ga-2">
            <v-btn
              variant="outlined"
              :disabled="selectedTopics.length === 0 || busy"
              @click="prepareSync(selectedTopics)"
            >
              {{ $t('consistency.kafkaConfig.actions.syncSelected') }}
              <span v-if="selectedTopics.length" class="ml-1">
                ({{ selectedTopics.length }})
              </span>
            </v-btn>
            <v-btn
              variant="outlined"
              :disabled="totalDriftedCount === 0 || busy"
              @click="prepareSync()"
            >
              {{ $t('consistency.kafkaConfig.actions.syncAll') }}
            </v-btn>
          </div>
        </v-col>
      </v-row>

      <v-divider class="my-4" />

      <div class="d-flex flex-column flex-lg-row align-lg-center ga-3">
        <div class="flex-grow-1">
          <div class="text-overline text-medium-emphasis">
            {{ $t('consistency.kafkaConfig.bootstrap.heading') }}
          </div>
          <div class="text-body-2 text-medium-emphasis">
            {{ $t('consistency.kafkaConfig.bootstrap.warning') }}
          </div>
        </div>
        <div class="d-flex flex-wrap ga-2">
          <v-btn
            :disabled="!selectedCluster || totalMissingCount === 0 || busy"
            color="warning"
            variant="tonal"
            @click="prepareBootstrap()"
          >
            {{ $t('consistency.kafkaConfig.bootstrap.create') }}
          </v-btn>
          <span class="text-caption text-medium-emphasis align-self-center">
            {{ bootstrapDisabledReason }}
          </span>
        </div>
      </div>
    </v-card-text>
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

  <template v-if="!loading">
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

    <v-card class="mb-2" variant="outlined">
      <v-card-text class="table-toolbar">
        <div class="d-flex flex-column flex-md-row align-md-center ga-3">
          <v-text-field
            :model-value="search"
            class="search-field"
            :label="$t('consistency.kafkaConfig.search')"
            prepend-inner-icon="mdi-magnify"
            density="compact"
            hide-details
            clearable
            variant="outlined"
            @update:model-value="onSearchChange"
          />
          <div class="d-flex align-center flex-wrap ga-2">
            <v-chip color="warning" size="small" variant="tonal">
              {{
                t('consistency.kafkaConfig.summary.drifted', {
                  count: displayedDriftedCount,
                })
              }}
            </v-chip>
            <v-chip color="error" size="small" variant="tonal">
              {{
                t('consistency.kafkaConfig.summary.missing', {
                  count: displayedMissingCount,
                })
              }}
            </v-chip>
          </div>
          <div class="ml-md-auto text-body-2 text-medium-emphasis text-no-wrap">
            {{
              t('consistency.kafkaConfig.pagination.range', {
                first: firstVisibleItem,
                last: lastVisibleItem,
                total: filteredInconsistencies.length,
              })
            }}
          </div>
          <div
            v-if="selectedTopics.length"
            class="d-flex align-center ga-1 text-body-2 text-no-wrap"
          >
            {{
              t('consistency.kafkaConfig.selection.count', {
                count: selectedTopics.length,
              })
            }}
            <v-btn size="small" variant="text" @click="clearSelection">
              {{ $t('consistency.kafkaConfig.selection.clear') }}
            </v-btn>
          </div>
        </div>
      </v-card-text>
      <v-divider />
      <v-table class="topics-table" density="comfortable" hover fixed-header>
        <thead>
          <tr>
            <th class="selection-column">
              <v-checkbox-btn
                :model-value="allPageTopicsSelected"
                :indeterminate="somePageTopicsSelected"
                :disabled="busy"
                :aria-label="$t('consistency.kafkaConfig.actions.selectPage')"
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
        <tbody v-if="paginatedInconsistencies.length > 0">
          <template
            v-for="topic in paginatedInconsistencies"
            :key="topicKey(topic)"
          >
            <tr>
              <td class="selection-column">
                <v-checkbox-btn
                  :model-value="selectedTopicKeySet.has(topicKey(topic))"
                  :disabled="busy || !topic.existsOnBroker"
                  :aria-label="topicKey(topic)"
                  @update:model-value="
                    (selected) => updateSelection(topic, selected)
                  "
                />
              </td>
              <td class="topic-column font-weight-medium">
                <span class="topic-name" :title="topic.qualifiedTopicName">
                  {{ topic.qualifiedTopicName }}
                </span>
              </td>
              <td class="topic-column">
                <span class="topic-name" :title="topic.kafkaTopicName">
                  {{ topic.kafkaTopicName }}
                </span>
              </td>
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
                  :color="topic.existsOnBroker ? 'primary' : 'warning'"
                  :disabled="busy"
                  @click="
                    topic.existsOnBroker
                      ? prepareSync([topic])
                      : prepareBootstrap(topic.clusterName)
                  "
                >
                  {{
                    $t(
                      topic.existsOnBroker
                        ? 'consistency.kafkaConfig.actions.sync'
                        : 'consistency.kafkaConfig.actions.create',
                    )
                  }}
                </v-btn>
              </td>
            </tr>
            <tr v-if="expandedTopicKeys.includes(topicKey(topic))">
              <td colspan="6" class="pa-4 bg-grey-lighten-5">
                <v-table density="compact">
                  <thead>
                    <tr>
                      <th>{{ $t('consistency.kafkaConfig.diffs.key') }}</th>
                      <th>
                        {{ $t('consistency.kafkaConfig.diffs.expected') }}
                      </th>
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
                          diff.actual ??
                          $t('consistency.kafkaConfig.diffs.unset')
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
              {{
                search
                  ? $t('consistency.kafkaConfig.noSearchResults')
                  : $t('consistency.kafkaConfig.noTopics')
              }}
            </th>
          </tr>
        </tbody>
      </v-table>
      <v-divider v-if="pageCount > 1" />
      <v-card-actions v-if="pageCount > 1" class="justify-center pa-3">
        <v-pagination
          v-model="page"
          :length="pageCount"
          :total-visible="7"
          density="comfortable"
        />
      </v-card-actions>
    </v-card>
  </template>
</template>

<style scoped lang="scss">
  .selection-column {
    width: 48px;
  }

  .control-card,
  .topics-table {
    overflow: hidden;
  }

  .table-toolbar {
    padding-block: 12px;
  }

  .search-field {
    max-width: 480px;
    min-width: 260px;
  }

  .topic-column {
    max-width: 420px;
  }

  .topic-name {
    display: block;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  @media (max-width: 960px) {
    .search-field {
      max-width: none;
      width: 100%;
    }
  }
</style>
