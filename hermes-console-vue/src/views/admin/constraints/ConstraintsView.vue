<script setup lang="ts">
  import { ref } from 'vue';
  import { useConstraints } from '@/composables/use-constraints/useConstraints';
  import { useI18n } from 'vue-i18n';
  import ConsoleAlert from '@/components/console-alert/ConsoleAlert.vue';
  import ConstraintsListing from '@/views/admin/constraints/constraints-listing/ConstraintsListing.vue';
  import LoadingSpinner from '@/components/loading-spinner/LoadingSpinner.vue';

  const { t } = useI18n();
  const topicFilter = ref<string>();
  const subscriptionFilter = ref<string>();

  const { topicConstraints, subscriptionConstraints, loading, error } =
    useConstraints();

  const breadcrumbsItems = [
    {
      title: t('constraints.breadcrumbs.home'),
      href: '/',
    },
    {
      title: t('constraints.breadcrumbs.title'),
    },
  ];
</script>

<template>
  <v-container>
    <v-row dense>
      <v-col md="12">
        <v-breadcrumbs :items="breadcrumbsItems" density="compact" />
        <loading-spinner v-if="loading" />
        <console-alert
          v-if="error"
          :title="$t('constraints.connectionError.title')"
          :text="$t('constraints.connectionError.text')"
          type="error"
        />
      </v-col>
    </v-row>
    <v-row dense>
      <v-col md="10">
        <p class="text-h4 font-weight-bold mb-3">
          {{ $t('constraints.topicConstraints.heading') }}
        </p>
      </v-col>
      <v-col md="2">
        <v-btn prepend-icon="mdi-plus" color="secondary" block>
          {{ $t('constraints.topicConstraints.actions.create') }}
        </v-btn>
      </v-col>
    </v-row>
    <v-row dense>
      <v-col md="12">
        <v-text-field
          single-line
          :label="$t('constraints.topicConstraints.actions.search')"
          density="compact"
          v-model="topicFilter"
          prepend-inner-icon="mdi-magnify"
        />
      </v-col>
    </v-row>
    <v-row dense>
      <v-col md="12">
        <constraints-listing
          v-if="topicConstraints"
          :constraints="topicConstraints"
          :filter="topicFilter"
        />
      </v-col>
    </v-row>
    <v-row dense class="mt-10">
      <v-col md="10">
        <p class="text-h4 font-weight-bold mb-3">
          {{ $t('constraints.subscriptionConstraints.heading') }}
        </p>
      </v-col>
      <v-col md="2">
        <v-btn prepend-icon="mdi-plus" color="secondary" block>
          {{ $t('constraints.subscriptionConstraints.actions.create') }}
        </v-btn>
      </v-col>
    </v-row>
    <v-row dense>
      <v-col md="12">
        <v-text-field
          single-line
          :label="$t('constraints.subscriptionConstraints.actions.search')"
          density="compact"
          v-model="subscriptionFilter"
          prepend-inner-icon="mdi-magnify"
        />
      </v-col>
    </v-row>
    <v-row dense>
      <v-col md="12">
        <constraints-listing
          v-if="subscriptionConstraints"
          :constraints="subscriptionConstraints"
          :filter="subscriptionFilter"
        />
      </v-col>
    </v-row>
  </v-container>
</template>

<style scoped lang="scss"></style>
