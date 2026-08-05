<script setup lang="ts">
  import { computed, ref, watch } from 'vue';
  import { useConsistencyStore } from '@/store/consistency/useConsistencyStore';
  import { useDialog } from '@/composables/dialog/use-dialog/useDialog';
  import { useI18n } from 'vue-i18n';
  import { useInconsistentTopics } from '@/composables/inconsistent-topics/use-inconsistent-topics/useInconsistentTopics';
  import { useNotificationsStore } from '@/store/app-notifications/useAppNotifications';
  import ConfirmationDialog from '@/components/confirmation-dialog/ConfirmationDialog.vue';
  import ConsoleAlert from '@/components/console-alert/ConsoleAlert.vue';
  import InconsistentGroupsListing from '@/views/admin/consistency/inconsistent-groups-listing/InconsistentGroupsListing.vue';
  import InconsistentTopicsListing from '@/views/admin/consistency/inconsistent-topics-listing/InconsistentTopicsListing.vue';
  import KafkaConfigInconsistenciesListing from '@/views/admin/consistency/kafka-config-inconsistencies-listing/KafkaConfigInconsistenciesListing.vue';
  import LoadingSpinner from '@/components/loading-spinner/LoadingSpinner.vue';

  const { t } = useI18n();
  const topicFilter = ref<string>();
  const groupFilter = ref<string>();
  const expandedSections = ref<string[]>([]);
  const orphanTopicsLoaded = ref(false);
  const kafkaSectionLoaded = ref(false);

  const {
    topics,
    loading,
    error,
    fetchInconsistentTopics,
    removeTopicsLocally,
    removeInconsistentTopic,
  } = useInconsistentTopics(false);
  const notificationStore = useNotificationsStore();

  const consistencyStore = useConsistencyStore();

  function checkConsistency() {
    consistencyStore.fetch();
  }

  async function loadOrphanTopics() {
    await fetchInconsistentTopics();
    orphanTopicsLoaded.value = error.value.fetchInconsistentTopics === null;
  }

  watch(expandedSections, (sections) => {
    if (
      sections.includes('orphan-topics') &&
      !orphanTopicsLoaded.value &&
      !loading.value
    ) {
      void loadOrphanTopics();
    }
    if (sections.includes('kafka-config')) kafkaSectionLoaded.value = true;
  });

  const breadcrumbsItems = [
    {
      title: t('consistency.breadcrumbs.home'),
      href: '/',
    },
    {
      title: t('consistency.breadcrumbs.title'),
    },
  ];

  const topicToDelete = ref();
  const selectedTopics = ref<string[]>([]);
  const batchProgress = ref<number>();
  const batchTotal = ref(0);
  const isBatchDeletionInProgress = computed(
    () => batchProgress.value !== undefined,
  );

  const {
    isDialogOpened: isRemoveDialogOpened,
    actionButtonEnabled: removeActionButtonEnabled,
    openDialog: openRemoveDialog,
    closeDialog: closeRemoveDialog,
    enableActionButton: enableRemoveActionButton,
    disableActionButton: disableRemoveActionButton,
  } = useDialog();

  const {
    isDialogOpened: isBatchRemoveDialogOpened,
    actionButtonEnabled: batchRemoveActionButtonEnabled,
    openDialog: openBatchRemoveDialog,
    closeDialog: closeBatchRemoveDialog,
    enableActionButton: enableBatchRemoveActionButton,
    disableActionButton: disableBatchRemoveActionButton,
  } = useDialog();

  async function deleteInconsistentTopic() {
    const topic = topicToDelete.value;
    disableRemoveActionButton();
    const isTopicRemoved = await removeInconsistentTopic(topic);
    enableRemoveActionButton();
    if (topicToDelete.value === topic) {
      closeRemoveDialog();
    }
    if (isTopicRemoved) {
      removeTopicsLocally([topic]);
      selectedTopics.value = selectedTopics.value.filter(
        (selectedTopic) => selectedTopic !== topic,
      );
    }
  }

  function openTopicRemoveDialog(topic: string) {
    topicToDelete.value = topic;
    openRemoveDialog();
  }

  async function deleteSelectedTopics() {
    const topicsToRemove = [...selectedTopics.value];
    const failedTopics: string[] = [];
    const successfulTopics: string[] = [];
    const concurrency = 5;
    let nextTopicIndex = 0;

    disableBatchRemoveActionButton();
    closeBatchRemoveDialog();
    batchProgress.value = 0;
    batchTotal.value = topicsToRemove.length;

    async function removeNextTopic() {
      while (nextTopicIndex < topicsToRemove.length) {
        const topic = topicsToRemove[nextTopicIndex++];
        if (await removeInconsistentTopic(topic, false)) {
          successfulTopics.push(topic);
          removeTopicsLocally([topic]);
        } else {
          failedTopics.push(topic);
        }
        batchProgress.value!++;
      }
    }

    await Promise.all(
      Array.from({ length: Math.min(concurrency, topicsToRemove.length) }, () =>
        removeNextTopic(),
      ),
    );

    await fetchInconsistentTopics();
    removeTopicsLocally(successfulTopics);
    selectedTopics.value = failedTopics.filter((topic) =>
      topics.value?.includes(topic),
    );
    await notificationStore.dispatchNotification({
      text: t('consistency.inconsistentTopics.batch.complete', {
        successful: successfulTopics.length,
        failed: failedTopics.length,
      }),
      type: failedTopics.length === 0 ? 'success' : 'warning',
    });
    batchProgress.value = undefined;
    batchTotal.value = 0;
    enableBatchRemoveActionButton();
  }
</script>

<template>
  <confirmation-dialog
    v-model="isRemoveDialogOpened"
    :actionButtonEnabled="removeActionButtonEnabled"
    :title="
      $t('consistency.inconsistentTopics.confirmationDialog.remove.title')
    "
    :text="
      t('consistency.inconsistentTopics.confirmationDialog.remove.text', {
        topicToDelete,
      })
    "
    @action="deleteInconsistentTopic"
    @cancel="closeRemoveDialog"
  />
  <confirmation-dialog
    v-model="isBatchRemoveDialogOpened"
    :actionButtonEnabled="batchRemoveActionButtonEnabled"
    :title="$t('consistency.inconsistentTopics.batch.confirmation.title')"
    :text="
      t('consistency.inconsistentTopics.batch.confirmation.text', {
        count: selectedTopics.length,
      })
    "
    @action="deleteSelectedTopics"
    @cancel="closeBatchRemoveDialog"
  />
  <v-container>
    <v-row dense>
      <v-col md="12">
        <v-breadcrumbs :items="breadcrumbsItems" density="compact" />
        <console-alert
          v-if="consistencyStore.error.fetchError"
          :title="$t('consistency.connectionError.title')"
          :text="$t('consistency.connectionError.text')"
          type="error"
        />
      </v-col>
    </v-row>
    <v-row dense>
      <v-col md="4">
        <p class="text-h5 font-weight-bold mb-3">
          {{ $t('consistency.inconsistentGroups.heading') }}
        </p>
      </v-col>
      <v-col class="text-right">
        <v-btn color="light-blue" @click="checkConsistency">
          {{ $t('consistency.inconsistentGroups.actions.check') }}
        </v-btn>
      </v-col>
    </v-row>
    <v-row>
      <v-col md="12" v-if="consistencyStore.fetchInProgress">
        <v-progress-linear
          :model-value="consistencyStore.progressPercent"
          :buffer-value="100"
          color="blue"
          data-testid="consistency-progress-bar"
        ></v-progress-linear>
      </v-col>
    </v-row>
    <v-row dense>
      <v-col md="12">
        <v-text-field
          single-line
          :label="$t('consistency.inconsistentGroups.actions.search')"
          density="compact"
          v-model="groupFilter"
          prepend-inner-icon="mdi-magnify"
        />
      </v-col>
    </v-row>
    <v-row dense>
      <v-col md="12">
        <inconsistent-groups-listing
          v-if="consistencyStore.groups"
          :inconsistent-groups="consistencyStore.groups"
          :filter="groupFilter"
        />
      </v-col>
    </v-row>
    <v-row dense class="mt-6">
      <v-col cols="12">
        <v-expansion-panels v-model="expandedSections" multiple>
          <v-expansion-panel value="orphan-topics">
            <v-expansion-panel-title>
              <span class="text-h6 font-weight-bold">
                {{ $t('consistency.inconsistentTopics.heading') }}
              </span>
            </v-expansion-panel-title>
            <v-expansion-panel-text>
              <template v-if="expandedSections.includes('orphan-topics')">
                <loading-spinner v-if="loading" />
                <console-alert
                  v-if="error.fetchInconsistentTopics"
                  :title="$t('consistency.connectionError.title')"
                  :text="$t('consistency.connectionError.text')"
                  type="error"
                />
                <v-btn
                  v-if="error.fetchInconsistentTopics"
                  class="mb-3"
                  variant="outlined"
                  @click="loadOrphanTopics"
                >
                  {{ $t('consistency.inconsistentTopics.actions.retry') }}
                </v-btn>
                <div
                  class="d-flex flex-column flex-md-row align-md-center mb-3 ga-3"
                >
                  <v-text-field
                    v-model="topicFilter"
                    class="flex-grow-1"
                    single-line
                    :label="$t('consistency.inconsistentTopics.actions.search')"
                    density="compact"
                    :disabled="isBatchDeletionInProgress"
                    prepend-inner-icon="mdi-magnify"
                    hide-details
                  />
                  <span data-testid="selected-inconsistent-topics-count">
                    {{
                      t('consistency.inconsistentTopics.selected', {
                        count: selectedTopics.length,
                      })
                    }}
                  </span>
                  <v-btn
                    color="red"
                    :disabled="
                      selectedTopics.length === 0 || isBatchDeletionInProgress
                    "
                    @click="openBatchRemoveDialog"
                  >
                    {{
                      $t(
                        'consistency.inconsistentTopics.actions.removeSelected',
                      )
                    }}
                  </v-btn>
                </div>
                <v-alert
                  v-if="isBatchDeletionInProgress"
                  type="info"
                  variant="tonal"
                  data-testid="inconsistent-topics-batch-progress"
                >
                  {{
                    t('consistency.inconsistentTopics.batch.progress', {
                      completed: batchProgress,
                      total: batchTotal,
                    })
                  }}
                </v-alert>
                <inconsistent-topics-listing
                  v-if="topics"
                  :inconsistentTopics="topics"
                  :filter="topicFilter"
                  v-model:selected-topics="selectedTopics"
                  :disabled="isBatchDeletionInProgress"
                  @remove="openTopicRemoveDialog"
                />
              </template>
            </v-expansion-panel-text>
          </v-expansion-panel>

          <v-expansion-panel value="kafka-config">
            <v-expansion-panel-title>
              <span class="text-h6 font-weight-bold">
                {{ $t('consistency.kafkaConfig.heading') }}
              </span>
            </v-expansion-panel-title>
            <v-expansion-panel-text>
              <kafka-config-inconsistencies-listing v-if="kafkaSectionLoaded" />
            </v-expansion-panel-text>
          </v-expansion-panel>
        </v-expansion-panels>
      </v-col>
    </v-row>
  </v-container>
</template>

<style scoped lang="scss"></style>
