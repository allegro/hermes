<script setup lang="ts">
  import { useTheme } from 'vuetify';
  import EnvironmentBadge from '@/components/environment-badge/EnviromentBadge.vue';
  import ThemeSwitch from '@/components/theme-switch/ThemeSwitch.vue';

  const theme = useTheme();
</script>

<template>
  <v-app-bar :elevation="2">
    <div class="header">
      <!-- TODO: navigate to home -->
      <div class="header-left">
        <router-link to="/" custom v-slot="{ navigate }">
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
        <!-- TODO: pass environment name from config -->
        <environment-badge environment-name="dev" />
      </div>
      <theme-switch />
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
