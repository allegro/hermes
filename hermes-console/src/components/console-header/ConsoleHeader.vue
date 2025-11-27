<script setup lang="ts">
  import { computed, onMounted, onBeforeUnmount, ref } from 'vue';
  import { useAppConfigStore } from '@/store/app-config/useAppConfigStore';
  import { useAuthStore } from '@/store/auth/useAuthStore';
  import { useI18n } from 'vue-i18n';
  import { useRouter } from 'vue-router';
  import { useTheme } from 'vuetify';
  import EnvironmentBadge from '@/components/environment-badge/EnviromentBadge.vue';
  import ThemeSwitch from '@/components/theme-switch/ThemeSwitch.vue';
  import SearchBar from '@/components/search-bar/SearchBar.vue';
  import CommandPalette from '@/components/command-palette/CommandPalette.vue';

  const { t } = useI18n();

  const router = useRouter();

  const theme = useTheme();
  const configStore = useAppConfigStore();
  const authStore = useAuthStore();

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

  function closeCommandPalette() {
    isCommandPaletteOpen.value = false;
  }

  function handleKeyDown(event: KeyboardEvent) {
    if (
      event.key === '/' &&
      !event.ctrlKey &&
      !event.metaKey &&
      !event.altKey
    ) {
      const tag = (event.target as HTMLElement | null)?.tagName?.toLowerCase();
      const isInputElement =
        tag === 'input' || tag === 'textarea' || tag === 'select';

      if (!isInputElement) {
        event.preventDefault();
        openCommandPalette();
      }
    }
  }

  onMounted(() => {
    window.addEventListener('keydown', handleKeyDown);
  });

  onBeforeUnmount(() => {
    window.removeEventListener('keydown', handleKeyDown);
  });
</script>

<template>
  <v-app-bar flat density="compact" color="surface">
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

      <div class="d-flex align-center flex-grow-1 ga-4 px-4">
        <search-bar @open="openCommandPalette" />
      </div>

      <div class="d-flex align-center ga-2 pr-2">
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

    <command-palette
      v-model="isCommandPaletteOpen"
      @update:modelValue="(value) => (isCommandPaletteOpen = value)"
    />
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
