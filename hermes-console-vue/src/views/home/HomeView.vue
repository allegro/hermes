<script setup lang="ts">
  import { useTheme } from 'vuetify';

  const adminViews: { title: string; to: string }[] = [
    { title: 'Consistency', to: '/consistency' },
    { title: 'Constraints', to: '/constraints' },
    { title: 'Readiness', to: '/readiness' },
  ];

  const theme = useTheme();
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
        <v-btn to="/groups" color="secondary" block>
          <v-icon left icon="mdi-cog"></v-icon>&nbsp;Console
        </v-btn>
      </v-col>
    </v-row>

    <v-row justify="center">
      <v-col cols="3">
        <v-btn to="/stats" color="accent" block>
          <v-icon left icon="mdi-chart-bar"></v-icon>&nbsp;Stats
        </v-btn>
      </v-col>
      <v-col cols="3">
        <v-btn to="/search" color="accent" block>
          <v-icon left icon="mdi-magnify"></v-icon>&nbsp;Search
        </v-btn>
      </v-col>
    </v-row>

    <v-row justify="center">
      <v-col cols="3">
        <v-btn block>
          <v-icon left icon="mdi-chart-multiple"></v-icon>&nbsp;Runtime
        </v-btn>
      </v-col>
      <v-col cols="3">
        <v-btn href="https://hermes-pubsub.readthedocs.io/" block>
          <v-icon left icon="mdi-book-open-variant"></v-icon>&nbsp;Docs
        </v-btn>
      </v-col>
    </v-row>

    <v-row justify="center">
      <v-col cols="6">
        <v-btn color="secondary" block>
          <v-icon left icon="mdi-security"></v-icon>&nbsp;Admin tools
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
