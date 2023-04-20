<script setup lang="ts">
  import { ref } from 'vue';
  import { useI18n } from 'vue-i18n';
  import { useInconsistentTopics } from '@/composables/use-inconsistent-topics/useInconsistentTopics';
  import ConsistencyBreadcrumbs from '@/views/admin/consistency/consistency-breadcrumbs/ConsistencyBreadcrumbs.vue';
  import ConsoleAlert from '@/components/console-alert/ConsoleAlert.vue';
  import InconsistentTopicsListing from '@/views/admin/consistency/inconsistent-topics-listing/InconsistentTopicsListing.vue';
  import LoadingSpinner from '@/components/loading-spinner/LoadingSpinner.vue';

  const { t } = useI18n();
  const topicFilter = ref<string>();

  const { topics, loading, error } = useInconsistentTopics();
</script>

<template>
  <v-container>
    <v-row dense>
      <v-col md="12">
        <consistency-breadcrumbs />
        <loading-spinner v-if="loading" />
        <console-alert
          v-if="error"
          :title="t('consistency.connectionError.title')"
          :text="t('consistency.connectionError.text')"
          type="error"
        />
      </v-col>
    </v-row>
    <v-row dense>
      <v-col md="10">
        <p class="text-h5 font-weight-bold mb-3">
          {{ t('consistency.inconsistentTopics.heading') }}
        </p>
      </v-col>
    </v-row>
    <v-row dense>
      <v-col md="12">
        <v-text-field
          single-line
          :label="t('consistency.inconsistentTopics.actions.search')"
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
        />
      </v-col>
    </v-row>
  </v-container>
</template>

<style scoped lang="scss"></style>
