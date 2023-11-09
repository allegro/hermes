<script setup lang="ts">
  import { ref } from 'vue';
  import { useFavorites } from '@/store/favorites/useFavorites';
  import { useI18n } from 'vue-i18n';
  import FavoriteSubscriptionsListing from '@/views/favorite/subscriptions/subscription-listing/FavoriteSubscriptionsListing.vue';

  const { t } = useI18n();

  const favorites = useFavorites();

  const filter = ref<string>();
  const breadcrumbsItems = [
    {
      title: t('favorites.breadcrumbs.home'),
      href: '/',
    },
    {
      title: t('favorites.breadcrumbs.subscriptions'),
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
          {{ t('favorites.subscriptions.heading') }}
        </p>
      </v-col>
    </v-row>
    <v-row dense>
      <v-col md="12">
        <v-text-field
          single-line
          :label="t('favorites.subscriptions.actions.search')"
          density="compact"
          v-model="filter"
          prepend-inner-icon="mdi-magnify"
        />
      </v-col>
    </v-row>
    <v-row dense>
      <v-col md="12">
        <favorite-subscriptions-listing
          v-if="favorites"
          :subscriptions="favorites.getSubscriptions"
          :filter="filter"
        />
      </v-col>
    </v-row>
  </v-container>
</template>

<style scoped lang="scss"></style>
