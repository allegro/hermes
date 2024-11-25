<script setup lang="ts">
  import { computed } from 'vue';
  import { useAppConfigStore } from '@/store/app-config/useAppConfigStore';
  import { useAuthStore } from '@/store/auth/useAuthStore';
  import { useI18n } from 'vue-i18n';
  import { useRouter } from 'vue-router';
  import { useTheme } from 'vuetify';
  import EnvironmentBadge from '@/components/environment-badge/EnviromentBadge.vue';
  import EnvironmentSwitch from '@/components/environment-switch/EnvironmentSwitch.vue';
  import ThemeSwitch from '@/components/theme-switch/ThemeSwitch.vue';

  const { t } = useI18n();

  const router = useRouter();

  const theme = useTheme();
  const configStore = useAppConfigStore();
  const authStore = useAuthStore();

  const isLoggedIn = computed(() => authStore.isUserAuthorized);
  const knownEnvironments = computed(
    () => configStore.appConfig?.console.knownEnvironments || [],
  );

  function logIn() {
    authStore.login(window.location.pathname);
  }

  function logout() {
    authStore.logout();
    router.go(0);
  }
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
        <v-divider vertical v-if="knownEnvironments.length > 0"></v-divider>
        <environment-switch :known-environments="knownEnvironments" />
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
