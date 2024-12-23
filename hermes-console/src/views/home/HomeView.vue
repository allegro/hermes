<script setup lang="ts">
  import { isAdmin } from '@/utils/roles-util';
  import { useAppConfigStore } from '@/store/app-config/useAppConfigStore';
  import { useRoles } from '@/composables/roles/use-roles/useRoles';
  import { useTheme } from 'vuetify';

  const adminViews: { title: string; to: string }[] = [
    { title: 'Consistency', to: '/ui/consistency' },
    { title: 'Constraints', to: '/ui/constraints' },
    { title: 'Readiness', to: '/ui/readiness' },
    { title: 'Inactive Topics', to: '/ui/inactive-topics' },
  ];

  const theme = useTheme();
  const configStore = useAppConfigStore();
  const roles = useRoles(null, null)?.roles;
</script>

<template>
  <v-container fill-height fluid class="mx-auto">
    <v-row class="home__logo" justify="center" cols="12">
      <img
        v-if="!theme.current.value.dark"
        src="@/assets/hermes-logo-full.png"
        alt="Hermes"
      />
      <img
        v-if="theme.current.value.dark"
        src="@/assets/hermes-logo-full-dark-theme.png"
        alt="Hermes"
      />
    </v-row>

    <v-row justify="center">
      <v-col cols="6">
        <v-btn to="/ui/groups" color="secondary" block>
          <v-icon left icon="mdi-cog"></v-icon>
          <span class="ml-1">{{ $t('homeView.links.console') }}</span>
        </v-btn>
      </v-col>
    </v-row>

    <v-row justify="center">
      <v-col cols="3">
        <v-btn to="/ui/favorite-topics" color="primary" block>
          <v-icon left icon="mdi-star"></v-icon>
          <span class="ml-1">{{ $t('homeView.links.favoriteTopics') }}</span>
        </v-btn>
      </v-col>
      <v-col cols="3">
        <v-btn to="/ui/favorite-subscriptions" color="primary" block>
          <v-icon left icon="mdi-star"></v-icon>
          <span class="ml-1">{{
            $t('homeView.links.favoriteSubscriptions')
          }}</span>
        </v-btn>
      </v-col>
    </v-row>

    <v-row justify="center">
      <v-col cols="3">
        <v-btn to="/ui/stats" color="accent" block>
          <v-icon left icon="mdi-chart-bar"></v-icon>
          <span class="ml-1">{{ $t('homeView.links.statistics') }}</span>
        </v-btn>
      </v-col>
      <v-col cols="3">
        <v-btn to="/ui/search" color="accent" block>
          <v-icon left icon="mdi-magnify"></v-icon>
          <span class="ml-1">{{ $t('homeView.links.search') }}</span>
        </v-btn>
      </v-col>
    </v-row>

    <v-row justify="center">
      <v-col cols="3">
        <v-btn
          block
          :href="configStore.loadedConfig.dashboard.metrics"
          target="_blank"
        >
          <v-icon left icon="mdi-chart-multiple"></v-icon>
          <span class="ml-1">{{ $t('homeView.links.runtime') }}</span>
        </v-btn>
      </v-col>
      <v-col cols="3">
        <v-btn
          block
          :href="configStore.loadedConfig.dashboard.docs"
          target="_blank"
        >
          <v-icon left icon="mdi-book-open-variant"></v-icon>
          <span class="ml-1">{{ $t('homeView.links.documentation') }}</span>
        </v-btn>
      </v-col>
    </v-row>

    <v-row justify="center" v-if="configStore.loadedConfig.costs.enabled">
      <v-col cols="6">
        <v-btn
          color="primary"
          block
          :href="configStore.loadedConfig.costs.globalDetailsUrl"
          target="_blank"
        >
          <v-icon left icon="mdi-currency-usd"></v-icon>
          <span class="ml-1">{{ $t('homeView.links.costs') }}</span>
        </v-btn>
      </v-col>
    </v-row>

    <v-row justify="center" v-if="isAdmin(roles)">
      <v-col cols="6">
        <v-btn color="secondary" block>
          <v-icon left icon="mdi-security"></v-icon>
          <span class="ml-1">{{ $t('homeView.links.adminTools') }}</span>
          &nbsp;<v-icon left icon="mdi-arrow-down"></v-icon>
          <v-menu activator="parent">
            <v-list>
              <v-list-item
                v-for="(item, index) in adminViews"
                :key="index"
                :value="index"
                :to="item.to"
              >
                <v-list-item-title>{{ item.title }}</v-list-item-title>
              </v-list-item>
            </v-list>
          </v-menu>
        </v-btn>
      </v-col>
    </v-row>
  </v-container>
</template>

<style lang="scss">
  .home__logo {
    margin-bottom: 4em;
    margin-top: 4em;
  }
</style>
