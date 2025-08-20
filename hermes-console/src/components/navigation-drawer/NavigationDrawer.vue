<script setup lang="ts">
  import { computed } from 'vue';
  import { useAppConfigStore } from '@/store/app-config/useAppConfigStore';
  import EnvironmentSelect from '@/components/environment-select/EnvironmentSelect.vue';

  const configStore = useAppConfigStore();
  const knownEnvironments = computed(
    () => configStore.appConfig?.console.knownEnvironments || [],
  );

  const navigationGroups = [
    {
      group: 'Console',
      items: [
        { title: 'Home', icon: 'mdi-home', to: '/ui' },
        { title: 'Groups', icon: 'mdi-file-tree', to: '/ui/groups' },
        {
          title: 'Favorite Topics',
          icon: 'mdi-file-table-box-outline',
          to: '/ui/favorite-topics',
        },
        {
          title: 'Favorite Subscriptions',
          icon: 'mdi-account-arrow-down-outline',
          to: '/ui/favorite-subscriptions',
        },
      ],
    },
    {
      group: 'Monitoring',
      items: [
        { title: 'Statistics', icon: 'mdi-chart-box-outline', to: '/ui/stats' },
        { title: 'Runtime', icon: 'mdi-chart-multiple', to: '/ui/runtime' },
        { title: 'Costs', icon: 'mdi-finance', to: '/ui/costs' },
      ],
    },
    {
      group: 'Admin',
      items: [
        {
          title: 'Consistency',
          icon: 'mdi-chart-histogram',
          to: '/ui/consistency',
        },
        { title: 'Constraints', icon: 'mdi-cogs', to: '/ui/constraints' },
        {
          title: 'Readiness',
          icon: 'mdi-arrow-right-drop-circle-outline',
          to: '/ui/readiness',
        },
      ],
    },
  ];
</script>

<template>
  <v-navigation-drawer floating color="surface" permanent class="border-e-sm">
    <div class="pa-2">
      <environment-select
        :known-environments="knownEnvironments"
        :is-current-environment-critical="
          configStore.loadedConfig.console.criticalEnvironment
        "
      />
    </div>

    <v-list density="compact" nav slim color="accent">
      <template
        v-for="(navigationGroup, navigationGroupIndex) in navigationGroups"
        :key="navigationGroupIndex"
      >
        <v-list-subheader class="text-subtitle-2 font-weight-bold">{{
          navigationGroup.group
        }}</v-list-subheader>

        <v-list-item
          v-for="(item, itemIndex) in navigationGroup.items"
          :key="itemIndex"
          :to="item.to"
        >
          <template v-slot:prepend>
            <v-icon :icon="item.icon" />
          </template>

          <v-list-item-title>{{ item.title }}</v-list-item-title>
        </v-list-item>
        <v-divider
          v-if="navigationGroupIndex + 1 < navigationGroups.length"
          class="mt-3 mb-3"
        />
      </template>
    </v-list>
  </v-navigation-drawer>
</template>

<style scoped lang="scss"></style>
