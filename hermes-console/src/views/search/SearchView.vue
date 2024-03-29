<script setup lang="ts">
  import { ref } from 'vue';
  import { SearchFilter, useSearch } from '@/composables/search/useSearch';
  import { useI18n } from 'vue-i18n';
  import ConsoleAlert from '@/components/console-alert/ConsoleAlert.vue';
  import LoadingSpinner from '@/components/loading-spinner/LoadingSpinner.vue';
  import SubscriptionSearchResults from '@/views/search/subscription-search-results/SubscriptionSearchResults.vue';
  import TopicSearchResults from '@/views/search/topic-search-results/TopicSearchResults.vue';

  const { t } = useI18n();

  const searchCollections = [
    { title: t('search.collection.subscriptions'), value: 'subscriptions' },
    { title: t('search.collection.topics'), value: 'topics' },
  ];
  const selectedSearchCollection = ref(searchCollections[0].value);

  const searchFilters = [
    { title: t('search.filter.endpoint'), value: SearchFilter.ENDPOINT },
    { title: t('search.filter.name'), value: SearchFilter.NAME },
    { title: t('search.filter.owner'), value: SearchFilter.OWNER },
  ];
  const selectedSearchFilter = ref(searchFilters[0].value);

  const searchPattern = ref('');

  const {
    topics,
    subscriptions,
    queryTopics,
    querySubscriptions,
    loading,
    error,
  } = useSearch();

  function search() {
    if (selectedSearchCollection.value === 'topics') {
      subscriptions.value = undefined;
      queryTopics(selectedSearchFilter.value, searchPattern.value);
    } else if (selectedSearchCollection.value === 'subscriptions') {
      topics.value = undefined;
      querySubscriptions(selectedSearchFilter.value, searchPattern.value);
    } else {
      throw Error(`Unknown search filter: ${selectedSearchCollection.value}`);
    }
  }
</script>

<template>
  <v-container>
    <console-alert
      v-if="error.fetchError"
      :title="$t('search.connectionError.title')"
      :text="$t('search.connectionError.text')"
      type="error"
    />
    <v-row>
      <p class="text-h3 mt-16">Search</p>
    </v-row>
    <v-form @submit.prevent="search">
      <v-row class="mt-8">
        <v-col md="2" cols="12">
          <v-select
            v-model="selectedSearchCollection"
            :items="searchCollections"
            variant="outlined"
          ></v-select>
        </v-col>
        <v-col md="2" cols="12">
          <v-select
            v-model="selectedSearchFilter"
            :items="searchFilters"
            variant="outlined"
          ></v-select>
        </v-col>
        <v-col md="7" cols="12" class="pr-0">
          <v-text-field
            v-model="searchPattern"
            label="regex pattern"
            required
            variant="outlined"
          ></v-text-field>
        </v-col>
        <v-col md="1" cols="12" class="text-right">
          <v-btn
            icon="mdi-magnify"
            type="submit"
            rounded="0"
            size="large"
          ></v-btn>
        </v-col>
      </v-row>
    </v-form>
    <v-row>
      <loading-spinner v-if="loading" />
    </v-row>
    <v-row dense>
      <v-col md="12">
        <TopicSearchResults v-if="topics" :topics="topics"></TopicSearchResults>
        <SubscriptionSearchResults
          v-if="subscriptions"
          :subscriptions="subscriptions"
        ></SubscriptionSearchResults>
      </v-col>
    </v-row>
  </v-container>
</template>

<style scoped lang="scss"></style>
