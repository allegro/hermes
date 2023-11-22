<script setup lang="ts">
  import { onMounted } from 'vue';
  import { useTheme } from 'vuetify';

  const THEME_LOCAL_STORAGE_KEY = 'hermes-console-theme';
  const theme = useTheme();

  onMounted(() => {
    theme.global.name.value = loadTheme() ?? 'light';
  });

  function toggleTheme() {
    if (theme.global.name.value === 'dark') {
      theme.global.name.value = 'light';
    } else {
      theme.global.name.value = 'dark';
    }
    storeTheme(theme.global.name.value);
  }

  function storeTheme(currentTheme: string) {
    window.localStorage.setItem(THEME_LOCAL_STORAGE_KEY, currentTheme);
  }

  function loadTheme(): string | null {
    return window.localStorage.getItem(THEME_LOCAL_STORAGE_KEY);
  }
</script>

<template>
  <v-tooltip text="Change theme">
    <template v-slot:activator="{ props }">
      <v-btn
        v-bind="props"
        :icon="
          theme.global.current.value.dark
            ? 'mdi-weather-night'
            : 'mdi-weather-sunny'
        "
        @click="toggleTheme"
      ></v-btn>
    </template>
  </v-tooltip>
</template>

<style scoped lang="scss"></style>
