<script setup lang="ts">
  import { computed } from 'vue';
  import { isAdmin, isAny } from '@/utils/roles-util';
  import { ref } from 'vue';
  import { useDialog } from '@/composables/dialog/use-dialog/useDialog';
  import { useGroups } from '@/composables/groups/use-groups/useGroups';
  import { useI18n } from 'vue-i18n';
  import { useRoles } from '@/composables/roles/use-roles/useRoles';
  import { useRouter } from 'vue-router';
  import ConfirmationDialog from '@/components/confirmation-dialog/ConfirmationDialog.vue';
  import ConsoleAlert from '@/components/console-alert/ConsoleAlert.vue';
  import GroupTopicsListing from '@/views/group-topics/group-topics-listing/GroupTopicsListing.vue';
  import LoadingSpinner from '@/components/loading-spinner/LoadingSpinner.vue';
  import TopicForm from '@/views/topic/topic-form/TopicForm.vue';

  const router = useRouter();
  const params = router.currentRoute.value.params as Record<string, string>;
  const { groupId } = params;
  const { t } = useI18n();

  const { groups, loading, error, removeGroup } = useGroups();

  const filter = ref<string>();

  const roles = useRoles(null, null).roles;

  const group = computed(() => {
    return (groups.value || [])?.find((i) => i.name === groupId);
  });

  const {
    isDialogOpened: isRemoveDialogOpened,
    actionButtonEnabled: removeActionButtonEnabled,
    openDialog: openRemoveDialog,
    closeDialog: closeRemoveDialog,
    enableActionButton: enableRemoveActionButton,
    disableActionButton: disableRemoveActionButton,
  } = useDialog();

  async function deleteGroup(groupId: string) {
    disableRemoveActionButton();
    const isGroupRemoved = await removeGroup(groupId);
    enableRemoveActionButton();
    closeRemoveDialog();
    if (isGroupRemoved) {
      router.push({ path: `/ui/groups` });
    }
  }

  const showTopicCreationForm = ref(false);
  function showTopicForm() {
    showTopicCreationForm.value = true;
  }
  function hideTopicForm() {
    showTopicCreationForm.value = false;
  }

  function pushToTopic(topic: string) {
    router.push({
      path: `/ui/groups/${groupId}/topics/${topic}`,
    });
  }

  const breadcrumbsItems = [
    {
      title: t('subscription.subscriptionBreadcrumbs.home'),
      href: '/',
    },
    {
      title: t('subscription.subscriptionBreadcrumbs.groups'),
      href: '/ui/groups',
    },
    {
      title: groupId,
      href: `/ui/groups/${groupId}`,
    },
  ];
</script>

<template>
  <confirmation-dialog
    v-model="isRemoveDialogOpened"
    :actionButtonEnabled="removeActionButtonEnabled"
    :title="$t('groups.confirmationDialog.remove.title')"
    :text="t('groups.confirmationDialog.remove.text', { groupId })"
    @action="deleteGroup(groupId)"
    @cancel="closeRemoveDialog"
  />
  <v-container>
    <v-row dense>
      <v-col md="12">
        <v-breadcrumbs :items="breadcrumbsItems" />
        <loading-spinner v-if="loading" />
        <console-alert
          v-if="error.fetchTopicNames || error.fetchGroupNames"
          :title="t('groups.connectionError.title')"
          :text="t('groups.connectionError.text')"
          type="error"
        />
      </v-col>
    </v-row>
    <v-row dense>
      <v-col>
        <v-card density="compact" class="d-flex flex-row justify-space-between">
          <v-card-text style="min-width: 70%">
            <p class="text-overline">{{ $t('groupTopics.title') }}</p>
            <p class="text-h4 font-weight-bold mb-2">
              {{ groupId }}
            </p>
          </v-card-text>
          <v-card-actions>
            <v-btn
              v-if="isAdmin(roles)"
              :disabled="group?.topics.length !== 0"
              color="red"
              prepend-icon="mdi-delete"
              @click="openRemoveDialog"
            >
              {{ $t('groups.actions.remove') }}
            </v-btn>
            <v-dialog
              v-model="showTopicCreationForm"
              min-width="800"
              :persistent="true"
            >
              <template #activator>
                <v-btn
                  :disabled="!isAny(roles)"
                  prepend-icon="mdi-plus"
                  density="comfortable"
                  @click="showTopicForm()"
                  >{{ $t('groups.actions.createTopic') }}</v-btn
                >
              </template>
              <v-card>
                <v-card-title>
                  <span class="text-h5">{{
                    $t('groups.actions.createTopic')
                  }}</span>
                </v-card-title>
                <v-card-text>
                  <TopicForm
                    operation="add"
                    :group="groupId"
                    :topic="null"
                    :roles="roles"
                    @created="pushToTopic"
                    @cancel="hideTopicForm"
                  />
                </v-card-text>
              </v-card>
            </v-dialog>
          </v-card-actions>
        </v-card>
      </v-col>
    </v-row>
    <v-row dense>
      <v-col md="12">
        <v-text-field
          single-line
          :label="t('groups.actions.search')"
          density="compact"
          v-model="filter"
          prepend-inner-icon="mdi-magnify"
        />
      </v-col>
    </v-row>
    <v-row dense>
      <v-col md="12">
        <group-topics-listing
          v-if="group && group.topics.length > 0"
          :group="group"
          :filter="filter"
        />
      </v-col>
    </v-row>
  </v-container>
</template>

<style scoped lang="scss"></style>
