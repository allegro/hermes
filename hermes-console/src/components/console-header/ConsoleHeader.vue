<script setup lang="ts">
  import { computed, ref } from 'vue';
  import { useAppConfigStore } from '@/store/app-config/useAppConfigStore';
  import { useAuthStore } from '@/store/auth/useAuthStore';
  import { useHotkey, useTheme } from 'vuetify';
  import { useI18n } from 'vue-i18n';
  import { useRouter } from 'vue-router';
  import EnvironmentBadge from '@/components/environment-badge/EnviromentBadge.vue';
  import SearchBar from '@/components/search-commander/search-bar/SearchBar.vue';
  import SearchCommander from '@/components/search-commander/SearchCommander.vue';
  import ThemeSwitch from '@/components/theme-switch/ThemeSwitch.vue';

  const { t } = useI18n();
  const router = useRouter();
  const theme = useTheme();
  const configStore = useAppConfigStore();
  const authStore = useAuthStore();
  useHotkey('cmd+k', openCommandPalette, {});

  const isLoggedIn = computed(() => authStore.isUserAuthorized);
  const isCommandPaletteOpen = ref(false);

  function logIn() {
    authStore.login(window.location.pathname);
  }

  function logout() {
    authStore.logout();
    router.go(0);
  }

  function openCommandPalette() {
    isCommandPaletteOpen.value = true;
  }
</script>

<template>
  <v-app-bar flat density="compact" color="surface">
    <v-row class="header">
      <v-col class="header-left">
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
      </v-col>

      <v-col class="d-flex align-center justify-center">
        <search-bar @open="openCommandPalette" hot-key="cmd+k" />
      </v-col>

      <v-col class="d-flex align-center ga-2 pr-2 justify-end">
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
      </v-col>
    </v-row>

    <search-commander
      v-model="isCommandPaletteOpen"
      @update:modelValue="(value: boolean) => (isCommandPaletteOpen = value)"
    />
  </v-app-bar>
</template>

<style scoped lang="scss">
  .header {
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
