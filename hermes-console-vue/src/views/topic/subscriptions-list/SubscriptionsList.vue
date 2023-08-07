<script setup lang="ts">
  import { computed } from 'vue';
  import { State } from '@/api/subscription';
  import type { Subscription } from '@/api/subscription';

  const props = defineProps<{
    groupId: string;
    topicName: string;
    subscriptions: Subscription[];
  }>();

  const statusTextColor: Record<State, String> = {
    [State.ACTIVE]: 'green',
    [State.PENDING]: 'orange',
    [State.SUSPENDED]: 'red',
  };

  const subscriptionItems = computed(() =>
    props.subscriptions.map((subscription) => {
      const currentUrl = window.location.href;
      return {
        name: subscription.name,
        color: statusTextColor[subscription.state],
        statusText: subscription.state,
        href: `${currentUrl}/subscriptions/${subscription.name}`,
      };
    }),
  );
</script>

<template>
  <v-expansion-panels>
    <v-expansion-panel
      :title="`${$t('topicView.subscriptions.title')} (${
        props.subscriptions.length
      })`"
    >
      <v-expansion-panel-text class="subscriptions-panel">
        <v-list open-strategy="single">
          <v-list-item
            v-for="subscription in subscriptionItems"
            :key="subscription.name"
            :href="subscription.href"
          >
            <v-list-item-title>{{ subscription.name }}</v-list-item-title>
            <template v-slot:append>
              <v-chip size="small" :color="subscription.color">{{
                subscription.statusText
              }}</v-chip>
            </template>
          </v-list-item>
        </v-list>
      </v-expansion-panel-text>
    </v-expansion-panel>
  </v-expansion-panels>
</template>

<style lang="scss" scoped>
  @use '@/settings';

  .v-list-item:not(:last-child) {
    border-bottom: settings.$list-item-border-thin-width
      settings.$list-item-border-style settings.$list-item-border-color;
  }

  .subscriptions-panel {
    margin: 0 -16px 0;
  }
</style>
