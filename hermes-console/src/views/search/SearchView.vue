<script setup lang="ts">
  import { onBeforeRouteUpdate, useRoute, useRouter } from 'vue-router';
  import { onMounted, ref } from 'vue';
  import { SearchFilter, useSearch } from '@/composables/search/useSearch';
  import { useI18n } from 'vue-i18n';
  import ConsoleAlert from '@/components/console-alert/ConsoleAlert.vue';
  import LoadingSpinner from '@/components/loading-spinner/LoadingSpinner.vue';
  import SubscriptionSearchResults from '@/views/search/subscription-search-results/SubscriptionSearchResults.vue';
  import TopicSearchResults from '@/views/search/topic-search-results/TopicSearchResults.vue';

  const { t } = useI18n();
  const route = useRoute();
  const router = useRouter();

  const searchCollectionValues = ['subscriptions', 'topics'];
  const searchCollections = [
    {
      title: t('search.collection.subscriptions'),
      value: searchCollectionValues[0],
    },
    { title: t('search.collection.topics'), value: searchCollectionValues[1] },
  ];
  const selectedSearchCollection = ref(
    searchCollectionValues.includes(route.query.collection)
      ? route.query.collection
      : searchCollectionValues[0],
  );

  const searchFilterValues = [
    SearchFilter.ENDPOINT,
    SearchFilter.NAME,
    SearchFilter.OWNER,
  ];
  const searchFilters = [
    { title: t('search.filter.endpoint'), value: searchFilterValues[0] },
    { title: t('search.filter.name'), value: searchFilterValues[1] },
    { title: t('search.filter.owner'), value: searchFilterValues[2] },
  ];
  const selectedSearchFilter = ref(
    searchFilterValues.includes(route.query.filter)
      ? route.query.filter
      : searchFilterValues[0],
  );

  const searchPattern = ref(route.query.pattern || '');

  const {
    topics,
    subscriptions,
    queryTopics,
    querySubscriptions,
    loading,
    error,
  } = useSearch();

  function updateQueryParams() {
    router.push({
      query: {
        collection: selectedSearchCollection.value,
        filter: selectedSearchFilter.value,
        pattern: searchPattern.value,
      },
    });
  }

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

  function searchAndUpdateQueryParams() {
    updateQueryParams();
    search();
  }

  onBeforeRouteUpdate((to) => {
    selectedSearchCollection.value = searchCollectionValues.includes(
      to.query.collection,
    )
      ? to.query.collection
      : searchCollectionValues[0];
    selectedSearchFilter.value = searchFilterValues.includes(to.query.filter)
      ? to.query.filter
      : searchFilterValues[0];
    searchPattern.value = to.query.pattern || '';
    search();
  });

  onMounted(() => {
    if (route.query.collection || route.query.filter || route.query.pattern) {
      search();
    }
  });
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
    <v-form @submit.prevent="searchAndUpdateQueryParams">
      <v-row class="mt-8">
        <v-col md="2" cols="12">
          <v-select
            v-model="selectedSearchCollection"
            label="collection"
            :items="searchCollections"
            variant="outlined"
          ></v-select>
        </v-col>
        <v-col md="2" cols="12">
          <v-select
            v-model="selectedSearchFilter"
            label="filter"
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
