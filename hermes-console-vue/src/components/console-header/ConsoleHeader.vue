<script setup lang="ts">
  import { ref, watch } from 'vue';
  import { useAppConfigStore } from '@/store/app-config/useAppConfigStore';
  import { useAuthStore } from '@/store/auth/useAuthStore';
  import { useI18n } from 'vue-i18n';
  import { useRoute } from 'vue-router';
  import { useTheme } from 'vuetify';
  import EnvironmentBadge from '@/components/environment-badge/EnviromentBadge.vue';
  import ThemeSwitch from '@/components/theme-switch/ThemeSwitch.vue';

  const { t } = useI18n();

  const theme = useTheme();
  const configStore = useAppConfigStore();
  const auth = useAuthStore();
  const route = useRoute();

  const isLoggedIn = ref(auth.isUserAuthorized);

  function logIn() {
    auth.login(window.location.pathname);
  }

  function logout() {
    auth.logout();
    isLoggedIn.value = false;
  }

  watch(route, async () => {
    isLoggedIn.value = auth.isUserAuthorized;
  });
</script>

<template>
  <v-app-bar :elevation="2" density="compact">
    <div class="header">
      <!-- TODO: navigate to home -->
      <div class="header-left">
        <router-link to="/ui" custom v-slot="{ navigate }">
          <img
            @click="navigate"
            v-if="!theme.current.value.dark"
            class="header__logo"
            src="@/assets/hermes-logo-header.png"
            alt="Hermes"
          />
          <img
            @click="navigate"
            v-if="theme.current.value.dark"
            class="header__logo"
            src="@/assets/hermes-logo-header-dark-theme.png"
            alt="Hermes"
          />
        </router-link>
        <environment-badge
          :environment-name="
            configStore.appConfig?.console.environmentName || ''
          "
          :is-critical-environment="
            configStore.appConfig?.console.criticalEnvironment || false
          "
        />
      </div>
      <div>
        <theme-switch />
        <v-btn
          v-if="configStore.loadedConfig.auth.oauth.enabled && !isLoggedIn"
          color="primary"
          variant="tonal"
          @click="logIn()"
        >
          {{ t('header.signIn') }}
        </v-btn>
        <v-btn
          v-if="configStore.loadedConfig.auth.oauth.enabled && isLoggedIn"
          variant="tonal"
          color="primary"
          @click="logout"
        >
          {{ t('header.logout') }}
        </v-btn>
      </div>
    </div>
  </v-app-bar>
</template>

<style scoped lang="scss">
  .header {
    display: flex;
    flex-direction: row;
    justify-content: space-between;
    width: 100%;

    &-left {
      align-items: center;
      display: flex;
      flex-direction: row;
      gap: 0.75rem;
      padding: 0 0.75rem;
    }

    &__logo {
      cursor: pointer;
      height: 30px;
    }

    &__name {
      font-size: 20px;
    }
  }
</style>
