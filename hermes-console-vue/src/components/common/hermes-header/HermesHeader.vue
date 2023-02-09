<script setup lang="ts"></script>

<template>
  <v-app-bar :elevation="2">
    <img class="header__logo" src="@/assets/hermes-logo.png" alt="Hermes" />
    <h1 class="header__name">hermes console</h1>
  </v-app-bar>
</template>

<style scoped lang="scss">
  .header__logo {
    height: 30px;
    padding: 0 1rem;
  }

  .header__name {
    font-size: 18px;
  }
</style>
