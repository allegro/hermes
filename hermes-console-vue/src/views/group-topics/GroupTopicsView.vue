<script setup lang="ts">
  import { computed } from 'vue';
  import { isAdmin } from '@/utils/roles-util';
  import { ref } from 'vue';
  import { useGroups } from '@/composables/groups/use-groups/useGroups';
  import { useI18n } from 'vue-i18n';
  import { useRoles } from '@/composables/roles/use-roles/useRoles';
  import { useRoute } from 'vue-router';
  import ConsoleAlert from '@/components/console-alert/ConsoleAlert.vue';
  import GroupTopicsListing from '@/views/group-topics/group-topics-listing/GroupTopicsListing.vue';
  import LoadingSpinner from '@/components/loading-spinner/LoadingSpinner.vue';

  const route = useRoute();
  const params = route.params as Record<string, string>;
  const { groupId } = params;
  const { t } = useI18n();

  const { groups, loading, error } = useGroups();

  const filter = ref<string>();

  const roles = useRoles(null, null).roles;

  const group = computed(() => {
    return (groups.value || [])?.find((i) => i.name === groupId);
  });

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
          <v-card-text>
            <p class="text-overline">Group</p>
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
              @click="$emit('remove')"
            >
              {{ $t('subscription.subscriptionMetadata.actions.remove') }}
            </v-btn>
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
