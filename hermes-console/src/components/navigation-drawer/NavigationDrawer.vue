<script setup lang="ts">
  import { isAdmin } from '@/utils/roles-util';
  import { useAppConfigStore } from '@/store/app-config/useAppConfigStore';
  import { useRoles } from '@/composables/roles/use-roles/useRoles';
  import { useRoute } from 'vue-router';

  import { computed, ref } from 'vue';
  import NavigationItem from '@/components/navigation-drawer/NavigationItem.vue';
  const route = useRoute();
  const routeName = computed(() => route.name);

  const configStore = useAppConfigStore();
  const roles = useRoles(null, null)?.roles;
  const rail = ref(true);

  function handleIconClick(url: string) {
    if (rail.value) {
      rail.value = false;
    } else if (url) {
      window.open(url, '_blank');
    }
  }
</script>

<template>
  <v-navigation-drawer :rail="rail" permanent @click="rail = false">
    <v-list>
      <v-list-item>
        <div class="d-flex align-center justify-center" style="width: 100%">
          <v-btn
            v-if="rail"
            icon="mdi-chevron-right"
            variant="text"
            @click.stop="rail = !rail"
          ></v-btn>
        </div>
        <div class="d-flex justify-end" style="width: 100%">
          <v-btn
            v-if="!rail"
            icon="mdi-chevron-left"
            variant="text"
            @click.stop="rail = !rail"
          ></v-btn>
        </div>
      </v-list-item>
    </v-list>

    <v-divider></v-divider>

    <v-list density="compact" nav>
      <navigation-item
        icon="mdi-cog"
        translation-key="homeView.links.console"
        name="groups"
        :current-route-name="routeName"
      />

      <v-list-group value="Favorites">
        <template v-slot:activator="{ props }">
          <navigation-item
            v-bind="props"
            icon="mdi-star"
            translation-key="homeView.links.favorites"
            name="favorites"
            :readonly="true"
          />
        </template>

        <navigation-item
          translation-key="homeView.links.subscriptions"
          name="favoriteSubscriptions"
          :current-route-name="routeName"
        />
        <navigation-item
          translation-key="homeView.links.topics"
          name="favoriteTopics"
          :current-route-name="routeName"
        />
      </v-list-group>
      <navigation-item
        icon="mdi-chart-bar"
        translation-key="homeView.links.statistics"
        name="stats"
        :current-route-name="routeName"
      />
      <navigation-item
        icon="mdi-magnify"
        translation-key="homeView.links.search"
        name="search"
        :current-route-name="routeName"
      />
      <navigation-item
        icon="mdi-chart-multiple"
        translation-key="homeView.links.runtime"
        name="runtime"
        :external-url="configStore.loadedConfig.dashboard.metrics"
        @icon-click="handleIconClick"
      />
      <navigation-item
        icon="mdi-book-open-variant"
        translation-key="homeView.links.documentation"
        name="docs"
        :external-url="configStore.loadedConfig.dashboard.docs"
        @icon-click="handleIconClick"
      />
      <navigation-item
        v-if="configStore.loadedConfig.costs.enabled"
        icon="mdi-currency-usd"
        translation-key="homeView.links.costs"
        name="costs"
        :external-url="configStore.loadedConfig.costs.globalDetailsUrl"
        @icon-click="handleIconClick"
      />
      <v-list-group value="Admin" v-if="isAdmin(roles)">
        <template v-slot:activator="{ props }">
          <navigation-item
            v-bind="props"
            translation-key="homeView.links.adminTools"
            name="adminTools"
            icon="mdi-security"
            :readonly="true"
          />
        </template>

        <navigation-item
          translation-key="homeView.links.readiness"
          name="readiness"
          :current-route-name="routeName"
        />
        <navigation-item
          translation-key="homeView.links.constraints"
          name="constraints"
          :current-route-name="routeName"
        />
        <navigation-item
          translation-key="homeView.links.consistency"
          name="consistency"
          :current-route-name="routeName"
        />
        <navigation-item
          translation-key="homeView.links.inactiveTopics"
          name="inactiveTopics"
          :current-route-name="routeName"
        />
      </v-list-group>
    </v-list>
  </v-navigation-drawer>
</template>
