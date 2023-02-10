<script setup lang="ts">
  import { useTheme } from 'vuetify';
  import EnvironmentBadge from '@/components/environment-badge/EnviromentBadge.vue';
  import ThemeSwitch from '@/components/theme-switch/ThemeSwitch.vue';

  const theme = useTheme();
</script>

<template>
  <v-app-bar :elevation="2">
    <div class="header">
      <div class="header-left">
        <img
          v-if="!theme.current.value.dark"
          class="header__logo"
          src="@/assets/hermes-logo.png"
          alt="Hermes"
        />
        <img
          v-if="theme.current.value.dark"
          class="header__logo"
          src="@/assets/hermes-logo-dark-theme.png"
          alt="Hermes"
        />
        <h1 class="header__name">Hermes console</h1>
        <!-- TODO: pass environment name from config -->
        <environment-badge environment-name="prod" />
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
      padding: 0 0.75rem;
      gap: 0.75rem;
    }

    &__logo {
      height: 30px;
    }

    &__name {
      font-size: 20px;
    }
  }
</style>
