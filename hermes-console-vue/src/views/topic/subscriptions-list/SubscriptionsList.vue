<script setup lang="ts">
  import { computed, ref } from 'vue';
  import { State } from '@/api/subscription';
  import SubscriptionForm from '@/views/subscription/subscription-form/SubscriptionForm.vue';
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
  const showSubscriptionCreationForm = ref(false);
  function showSubscriptionForm() {
    showSubscriptionCreationForm.value = true;
  }
  function hideSubscriptionForm() {
    showSubscriptionCreationForm.value = false;
  }
</script>

<template>
  <v-expansion-panels>
    <v-expansion-panel
      :title="`${$t('topicView.subscriptions.title')} (${
        props.subscriptions.length
      })`"
    >
      <v-expansion-panel-text class="d-flex flex-row subscriptions-panel">
        <div class="d-flex justify-end mr-4 mb-1">
          <v-dialog
            v-model="showSubscriptionCreationForm"
            min-width="800"
            :persistent="true"
          >
            <template #activator>
              <v-btn
                icon="mdi-plus"
                density="comfortable"
                @click="showSubscriptionForm()"
              ></v-btn>
            </template>
            <v-card>
              <v-card-title>
                <span class="text-h5">Create subscription</span>
              </v-card-title>
              <v-card-text>
                <SubscriptionForm
                  operation="add"
                  :topic="props.topicName"
                  @cancel="hideSubscriptionForm()"
                />
              </v-card-text>
            </v-card>
          </v-dialog>
        </div>

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
