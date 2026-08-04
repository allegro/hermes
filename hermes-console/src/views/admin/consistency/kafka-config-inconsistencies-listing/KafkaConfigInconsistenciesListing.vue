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
  const search = ref('');
  const page = ref(1);
  const dryRun = ref(true);
  const reviewing = ref(false);
  const applying = ref(false);
  const itemsPerPage = 50;

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
  const displayedInconsistencies = computed(
    () => syncDryRun.value?.inconsistencies ?? inconsistencies.value,
  );
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
  const driftedCount = computed(
    () => inconsistencies.value.filter((topic) => topic.existsOnBroker).length,
  );
  const missingCount = computed(
    () => inconsistencies.value.length - driftedCount.value,
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

  async function runReview(topics?: InconsistentKafkaTopic[]) {
    reviewing.value = true;
    const reviewed = await reviewSync(topics);
    reviewing.value = false;
    if (reviewed) {
      dryRun.value = true;
      page.value = 1;
    }
  }

  async function applySync() {
    applying.value = true;
    await applyReviewedSync();
    applying.value = false;
    selectedTopicKeys.value = [];
    dryRun.value = true;
    page.value = 1;
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
    if (applied) {
      page.value = 1;
      closeBootstrapDialog();
    }
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
              :disabled="selectedTopics.length === 0 || busy || !dryRun"
              @click="runReview(selectedTopics)"
            >
              {{ $t('consistency.kafkaConfig.actions.syncSelected') }}
              <span v-if="selectedTopics.length" class="ml-1">
                ({{ selectedTopics.length }})
              </span>
            </v-btn>
            <v-btn
              variant="outlined"
              :disabled="driftedCount === 0 || busy || !dryRun"
              @click="runReview()"
            >
              {{ $t('consistency.kafkaConfig.actions.syncAll') }}
            </v-btn>
            <v-switch
              v-model="dryRun"
              class="dry-run-switch"
              :label="$t('consistency.kafkaConfig.dryRun')"
              :disabled="
                !syncDryRun || syncDryRun.inconsistencies.length === 0 || busy
              "
              color="primary"
              density="compact"
              hide-details
            />
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
            variant="outlined"
            :disabled="!selectedCluster || busy"
            @click="runBootstrapReview"
          >
            {{ $t('consistency.kafkaConfig.bootstrap.review') }}
          </v-btn>
          <v-btn
            color="warning"
            variant="tonal"
            :disabled="
              !bootstrapDryRun ||
              bootstrapDryRun.topicNames.length === 0 ||
              busy
            "
            @click="openBootstrapDialog"
          >
            {{ $t('consistency.kafkaConfig.bootstrap.apply') }}
          </v-btn>
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
                count: driftedCount,
              })
            }}
          </v-chip>
          <v-chip color="error" size="small" variant="tonal">
            {{
              t('consistency.kafkaConfig.summary.missing', {
                count: missingCount,
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
      <tbody v-if="paginatedInconsistencies.length > 0">
        <template
          v-for="topic in paginatedInconsistencies"
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

<style scoped lang="scss">
  .selection-column {
    width: 48px;
  }

  .control-card,
  .topics-table {
    overflow: hidden;
  }

  .dry-run-switch {
    flex: 0 0 auto;
    margin-inline: 4px;
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
