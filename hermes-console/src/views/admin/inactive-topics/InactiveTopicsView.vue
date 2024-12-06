<script setup lang="ts">
import {useI18n} from 'vue-i18n';
import ConsoleAlert from '@/components/console-alert/ConsoleAlert.vue';
import LoadingSpinner from '@/components/loading-spinner/LoadingSpinner.vue';
import {useInactiveTopics} from "@/composables/inactive-topics/use-inactive-topics/useInactiveTopics";
import InactiveTopicsListing from "@/views/admin/inactive-topics/inactive-topics-listing/InactiveTopicsListing.vue";

const {t} = useI18n();

// TODO

const {
  inactiveTopics,
  loading,
  error,
} = useInactiveTopics();

</script>

<template>
  <v-container>
    <v-row dense>
      <v-col md="12">
        <v-breadcrumbs :items="breadcrumbsItems" density="compact"/>
        <loading-spinner v-if="loading"/>
        <console-alert
            v-if="error.fetchConstraints"
            :title="$t('constraints.connectionError.title')"
            :text="$t('constraints.connectionError.text')"
            type="error"
        />
      </v-col>
    </v-row>
    <v-row dense>
      <v-col md="10">
        <p class="text-h4 font-weight-bold mb-3">
          {{ $t('inactiveTopics.heading') }}
        </p>
      </v-col>
    </v-row>
    <v-row dense>
      <v-col md="12">
        <inactive-topics-listing
            v-if="inactiveTopics"
            :inactive-topics="inactiveTopics"
        />
      </v-col>
    </v-row>
  </v-container>
</template>

<style scoped lang="scss"></style>
