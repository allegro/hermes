<script setup lang="ts">
  import { ref } from 'vue';
  import { useFavorites } from '@/store/favorites/useFavorites';
  import { useI18n } from 'vue-i18n';
  import FavoriteTopicsListing from '@/views/favorite/topics/topic-listing/FavoriteTopicsListing.vue';

  const { t } = useI18n();

  const favorites = useFavorites();

  const filter = ref<string>();
  const breadcrumbsItems = [
    {
      title: t('favorites.breadcrumbs.home'),
      href: '/',
    },
    {
      title: t('favorites.breadcrumbs.topics'),
    },
  ];
</script>

<template>
  <v-container>
    <v-row dense>
      <v-col md="12">
        <v-breadcrumbs :items="breadcrumbsItems" density="compact" />
      </v-col>
    </v-row>
    <v-row dense>
      <v-col md="10">
        <p class="text-h4 font-weight-bold mb-3">
          {{ t('favorites.topics.heading') }}
        </p>
      </v-col>
    </v-row>
    <v-row dense>
      <v-col md="12">
        <v-text-field
          variant="outlined"
          base-color="rgba(var(--v-border-color), 0.31)"
          single-line
          :label="t('favorites.topics.actions.search')"
          density="compact"
          v-model="filter"
          prepend-inner-icon="mdi-magnify"
        />
      </v-col>
    </v-row>
    <v-row dense>
      <v-col md="12">
        <favorite-topics-listing
          v-if="favorites"
          :topics="favorites.getTopics"
          :filter="filter"
        />
      </v-col>
    </v-row>
  </v-container>
</template>

<style scoped lang="scss"></style>
