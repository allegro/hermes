<script setup lang="ts">
  import { useGroups } from '@/composables/use-groups/useGroups';
  import { useI18n } from 'vue-i18n';
  import ConsoleAlert from '@/components/console-alert/ConsoleAlert.vue';
  import GroupBreadcrumbs from '@/views/groups/group-breadcrumbs/GroupBreadcrumbs.vue';
  import LoadingSpinner from '@/components/loading-spinner/LoadingSpinner.vue';

  const { groups, loading, error } = useGroups();
  const { t } = useI18n();
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
      <ul>
        <li v-for="group in groups" :key="group">
          {{ group.name }} ({{ group.topics.length }})
        </li>
      </ul>
    </v-row>
  </v-container>
</template>

<style scoped lang="scss"></style>
