<script setup lang="ts">
  import { computed, ref } from 'vue';
  import { useConsistencyStore } from '@/store/consistency/useConsistencyStore';
  import { useDialog } from '@/composables/dialog/use-dialog/useDialog';
  import { useI18n } from 'vue-i18n';
  import { useInconsistentTopics } from '@/composables/inconsistent-topics/use-inconsistent-topics/useInconsistentTopics';
  import { useNotificationsStore } from '@/store/app-notifications/useAppNotifications';
  import ConfirmationDialog from '@/components/confirmation-dialog/ConfirmationDialog.vue';
  import ConsoleAlert from '@/components/console-alert/ConsoleAlert.vue';
  import InconsistentGroupsListing from '@/views/admin/consistency/inconsistent-groups-listing/InconsistentGroupsListing.vue';
  import InconsistentTopicsListing from '@/views/admin/consistency/inconsistent-topics-listing/InconsistentTopicsListing.vue';
  import LoadingSpinner from '@/components/loading-spinner/LoadingSpinner.vue';

  const { t } = useI18n();
  const topicFilter = ref<string>();
  const groupFilter = ref<string>();

  const {
    topics,
    loading,
    error,
    fetchInconsistentTopics,
    removeTopicsLocally,
    removeInconsistentTopic,
  } = useInconsistentTopics();
  const notificationStore = useNotificationsStore();

  const consistencyStore = useConsistencyStore();

  function checkConsistency() {
    consistencyStore.fetch();
  }

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
        <loading-spinner v-if="loading" />
        <console-alert
          v-if="
            error.fetchInconsistentTopics || consistencyStore.error.fetchError
          "
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
    <v-row dense>
      <v-col md="8">
        <p class="text-h5 font-weight-bold mb-3">
          {{ $t('consistency.inconsistentTopics.heading') }}
        </p>
      </v-col>
      <v-col class="text-right">
        <span class="mr-4" data-testid="selected-inconsistent-topics-count">
          {{
            t('consistency.inconsistentTopics.selected', {
              count: selectedTopics.length,
            })
          }}
        </span>
        <v-btn
          color="red"
          :disabled="selectedTopics.length === 0 || isBatchDeletionInProgress"
          @click="openBatchRemoveDialog"
        >
          {{ $t('consistency.inconsistentTopics.actions.removeSelected') }}
        </v-btn>
      </v-col>
    </v-row>
    <v-row dense>
      <v-col md="12">
        <v-text-field
          single-line
          :label="$t('consistency.inconsistentTopics.actions.search')"
          density="compact"
          v-model="topicFilter"
          :disabled="isBatchDeletionInProgress"
          prepend-inner-icon="mdi-magnify"
        />
      </v-col>
    </v-row>
    <v-row dense>
      <v-col md="12">
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
      </v-col>
    </v-row>
  </v-container>
</template>

<style scoped lang="scss"></style>
