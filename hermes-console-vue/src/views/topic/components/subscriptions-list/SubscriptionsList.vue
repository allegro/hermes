<script setup lang="ts">
  import { computed } from 'vue';

  const props = defineProps<{
    subscriptions: string[];
  }>();

  const subscriptionItems = computed(() =>
    props.subscriptions.map((subscription) => {
      return {
        name: subscription,
        color: 'green',
        statusText: 'active',
        href: `a/subscriptions/${subscription}`,
      };
    }),
  );
</script>

<template>
  <v-expansion-panels>
    <v-expansion-panel title="Subscriptions">
      <v-expansion-panel-text class="expansion-panel__text">
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

<style lang="scss" scoped></style>
