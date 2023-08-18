<script setup lang="ts">
  import { useAppConfigStore } from '@/store/app-config/useAppConfigStore';
  import { useAuthStore } from '@/store/auth/useAuthStore';
  import { useRoute, useRouter } from 'vue-router';
  import LoadingSpinner from '@/components/loading-spinner/LoadingSpinner.vue';

  const configStore = useAppConfigStore();
  const auth = useAuthStore();
  const route = useRoute();
  const router = useRouter();

  auth.exchangeCodeForTokenWithPKCE(String(route.query.code));
  if (configStore.loadedConfig && route.query.code) {
    auth.exchangeCodeForTokenWithPKCE(String(route.query.code)).then(() => {
      router.push(String(route.query.state));
    });
  }
</script>

<template>
  <loading-spinner />
</template>

<style scoped lang="scss"></style>
