<script setup lang="ts">
  import { isAny } from '@/utils/roles-util';
  import { ref } from 'vue';
  import { useGroups } from '@/composables/groups/use-groups/useGroups';
  import { useI18n } from 'vue-i18n';
  import { useRoles } from '@/composables/roles/use-roles/useRoles';
  import { useRouter } from 'vue-router';
  import ConsoleAlert from '@/components/console-alert/ConsoleAlert.vue';
  import GroupForm from '@/views/groups/group-form/GroupForm.vue';
  import GroupListing from '@/views/groups/group-listing/GroupListing.vue';
  import LoadingSpinner from '@/components/loading-spinner/LoadingSpinner.vue';

  const { groups, loading, error, createGroup } = useGroups();
  const { t } = useI18n();
  const router = useRouter();

  const roles = useRoles(null, null)?.roles;

  const filter = ref<string>();
  const createGroupDialogOpen = ref(false);
  const breadcrumbsItems = [
    {
      title: t('subscription.subscriptionBreadcrumbs.home'),
      href: '/',
    },
    {
      title: t('subscription.subscriptionBreadcrumbs.groups'),
      href: '/ui/groups',
    },
  ];

  const onCreateGroup = async (groupId: string) => {
    const succeeded = await createGroup(groupId);
    if (succeeded) {
      router.go(0);
    }
  };
</script>

<template>
  <v-container>
    <v-row dense>
      <v-col md="12">
        <v-breadcrumbs :items="breadcrumbsItems" density="compact" />
        <loading-spinner v-if="loading" />
        <console-alert
          v-if="error.fetchGroupNames || error.fetchTopicNames"
          :title="t('groups.connectionError.title')"
          :text="t('groups.connectionError.text')"
          type="error"
        />
      </v-col>
    </v-row>
    <v-row dense>
      <v-col md="10">
        <p class="text-h4 font-weight-bold mb-3">
          {{ t('groups.heading') }}
        </p>
      </v-col>
      <v-col md="2">
        <v-btn
          prepend-icon="mdi-folder-plus-outline"
          color="secondary"
          block
          @click="createGroupDialogOpen = true"
          v-if="isAny(roles)"
        >
          {{ t('groups.actions.create') }}
        </v-btn>
        <group-form
          operation="create"
          v-model:dialog-open="createGroupDialogOpen"
          @create="onCreateGroup"
        />
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
        <group-listing
          v-if="groups && groups.length > 0"
          :groups="groups"
          :filter="filter"
        />
      </v-col>
    </v-row>
  </v-container>
</template>

<style scoped lang="scss"></style>
