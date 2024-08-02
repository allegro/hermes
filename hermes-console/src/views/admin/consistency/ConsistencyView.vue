<script setup lang="ts">
  import { ref } from 'vue';
  import { useConsistencyStore } from '@/store/consistency/useConsistencyStore';
  import { useDialog } from '@/composables/dialog/use-dialog/useDialog';
  import { useI18n } from 'vue-i18n';
  import { useInconsistentTopics } from '@/composables/inconsistent-topics/use-inconsistent-topics/useInconsistentTopics';
  import { useRouter } from 'vue-router';
  import ConfirmationDialog from '@/components/confirmation-dialog/ConfirmationDialog.vue';
  import ConsoleAlert from '@/components/console-alert/ConsoleAlert.vue';
  import InconsistentGroupsListing from '@/views/admin/consistency/inconsistent-groups-listing/InconsistentGroupsListing.vue';
  import InconsistentTopicsListing from '@/views/admin/consistency/inconsistent-topics-listing/InconsistentTopicsListing.vue';
  import LoadingSpinner from '@/components/loading-spinner/LoadingSpinner.vue';

  const { t } = useI18n();
  const topicFilter = ref<string>();
  const groupFilter = ref<string>();

  const { topics, loading, error, removeInconsistentTopic } =
    useInconsistentTopics();

  const router = useRouter();

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

  const {
    isDialogOpened: isRemoveDialogOpened,
    actionButtonEnabled: removeActionButtonEnabled,
    openDialog: openRemoveDialog,
    closeDialog: closeRemoveDialog,
    enableActionButton: enableRemoveActionButton,
    disableActionButton: disableRemoveActionButton,
  } = useDialog();

  async function deleteInconsistentTopic() {
    disableRemoveActionButton();
    const isTopicRemoved = await removeInconsistentTopic(topicToDelete.value);
    enableRemoveActionButton();
    closeRemoveDialog();
    if (isTopicRemoved) {
      router.go(0);
    }
  }

  function openTopicRemoveDialog(topic: string) {
    topicToDelete.value = topic;
    openRemoveDialog();
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
      <v-col md="10">
        <p class="text-h5 font-weight-bold mb-3">
          {{ $t('consistency.inconsistentTopics.heading') }}
        </p>
      </v-col>
    </v-row>
    <v-row dense>
      <v-col md="12">
        <v-text-field
          single-line
          :label="$t('consistency.inconsistentTopics.actions.search')"
          density="compact"
          v-model="topicFilter"
          prepend-inner-icon="mdi-magnify"
        />
      </v-col>
    </v-row>
    <v-row dense>
      <v-col md="12">
        <inconsistent-topics-listing
          v-if="topics"
          :inconsistentTopics="topics"
          :filter="topicFilter"
          @remove="openTopicRemoveDialog"
        />
      </v-col>
    </v-row>
  </v-container>
</template>

<style scoped lang="scss"></style>
