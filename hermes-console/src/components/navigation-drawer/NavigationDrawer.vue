<script setup lang="ts">
  import { isAdmin } from '@/utils/roles-util';
  import { useAppConfigStore } from '@/store/app-config/useAppConfigStore';
  import { useAuthStore } from '@/store/auth/useAuthStore';
  import { useRoles } from '@/composables/roles/use-roles/useRoles';
  import { useRoute } from 'vue-router';

  import { computed, ref } from 'vue';
  import NavigationItem from '@/components/navigation-drawer/NavigationItem.vue';
  const authStore = useAuthStore();
  const isLoggedIn = computed(() => authStore.isUserAuthorized);
  const userData = computed(() => authStore.userData);
  const route = useRoute();
  const routeName = computed(() => route.name);

  const configStore = useAppConfigStore();
  const roles = useRoles(null, null)?.roles;
  const rail = ref(false);
</script>

<template>
  <v-navigation-drawer :rail="rail" permanent @click="rail = false">
    <template v-if="!isLoggedIn">
      <v-list>
        <v-list-item
          :prepend-avatar="`https://alleavatar.allegrogroup.com/${userData.user_name}.jpg`"
          :title="isLoggedIn ? userData.full_name : 'Logged out'"
        >
          <template v-slot:append>
            <v-btn
              icon="mdi-chevron-left"
              variant="text"
              @click.stop="rail = !rail"
            ></v-btn>
          </template>
        </v-list-item>
      </v-list>

      <v-divider></v-divider>
    </template>
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
            prepend-icon="mdi-star"
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
      />
      <navigation-item
        icon="mdi-book-open-variant"
        translation-key="homeView.links.documentation"
        name="docs"
        :external-url="configStore.loadedConfig.dashboard.docs"
      />
      <navigation-item
        icon="mdi-currency-usd"
        translation-key="homeView.links.costs"
        name="costs"
        :external-url="configStore.loadedConfig.costs.globalDetailsUrl"
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
      </v-list-group>
    </v-list>
  </v-navigation-drawer>
</template>
