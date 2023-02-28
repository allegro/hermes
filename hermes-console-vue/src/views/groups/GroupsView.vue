<script setup lang="ts">
  import { ref } from 'vue';
  import { useGroups } from '@/composables/use-groups/useGroups';
  import { useI18n } from 'vue-i18n';
  import ConsoleAlert from '@/components/console-alert/ConsoleAlert.vue';
  import GroupBreadcrumbs from '@/views/groups/group-breadcrumbs/GroupBreadcrumbs.vue';
  import GroupListing from '@/views/groups/group-listing/GroupListing.vue';
  import LoadingSpinner from '@/components/loading-spinner/LoadingSpinner.vue';

  const { groups, loading, error } = useGroups();
  const { t } = useI18n();

  const filter = ref<string>();
</script>

<template>
  <v-container>
    <v-row dense>
      <v-col md="12">
        <group-breadcrumbs />
        <loading-spinner v-if="loading" />
        <console-alert
          v-if="error"
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
        <v-btn prepend-icon="mdi-folder-plus-outline" color="secondary" block>
          {{ t('groups.actions.create') }}
        </v-btn>
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
