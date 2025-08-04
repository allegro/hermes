<script setup lang="ts">
  import { defineProps, defineEmits } from 'vue';
  import { useRouter } from 'vue-router';
  const router = useRouter();

  const props = defineProps<{
    icon?: string;
    translationKey: string;
    name: string;
    currentRouteName?: string;
    readonly?: boolean;
    externalUrl?: string;
  }>();

  const emit = defineEmits(['icon-click']);

  function navigateToRoute() {
    if (props.readonly || props.externalUrl) {
      return;
    }
    router.push({ name: props.name });
  }

  function handleIconClick(event: Event) {
    if (props.externalUrl) {
      emit('icon-click', props.externalUrl);
      event.stopPropagation();
    }
  }
</script>

<template>
  <v-list-item
    :active="currentRouteName === name"
    :value="name"
    @click="navigateToRoute"
  >
    <template #prepend>
      <v-btn
        v-if="externalUrl"
        :icon="icon"
        variant="text"
        @click.stop="handleIconClick"
        size="small"
        style="justify-content: flex-start"
      />
      <v-icon v-else-if="icon" :icon="icon" />
    </template>
    <template v-if="!externalUrl">
      <v-list-item-title class="nav-title nav-title-align">{{
        $t(translationKey)
      }}</v-list-item-title>
    </template>
    <template v-else>
      <a
        :href="externalUrl"
        target="_blank"
        class="nav-title-link nav-title-align"
      >
        <v-list-item-title class="nav-title">{{
          $t(translationKey)
        }}</v-list-item-title>
      </a>
    </template>
  </v-list-item>
</template>

<style scoped lang="scss">
  a {
    text-decoration: none;
    color: inherit;
  }
  .nav-title {
    padding-left: 8px;
    display: block;
  }
  .nav-title-link {
    margin-left: 3%;
    display: block;
    width: 100%;
  }
  .nav-title-align {
    display: flex;
    align-items: center;
    width: 100%;
    min-width: 0;
    padding-left: 8px;
    box-sizing: border-box;
  }
</style>
