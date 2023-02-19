<script setup lang="ts">
  import { State } from '@/api/subscription';
  import TooltipIcon from '@/components/tooltip-icon/TooltipIcon.vue';
  import type { Subscription } from '@/api/subscription';

  const props = defineProps<{
    subscription: Subscription;
    authorized: boolean;
  }>();
</script>

<template>
  <v-card density="compact">
    <v-card-item>
      <p class="text-overline">Subscription</p>
      <p class="text-h4 font-weight-bold">
        {{ props.subscription.name }}
        <v-chip
          :color="props.subscription.state === State.ACTIVE ? 'green' : 'red'"
          size="small"
        >
          {{ props.subscription.state }}
        </v-chip>
      </p>
      <p class="text-subtitle-1">
        {{ props.subscription.endpoint }}
      </p>
    </v-card-item>
    <v-card-text>
      <p class="text-subtitle-2">{{ props.subscription.description }}</p>
    </v-card-text>
    <v-divider class="mx-4 mb-1"></v-divider>
    <v-card-actions class="d-flex subscription-header__actions">
      <div class="d-flex flex-row">
        <v-btn prepend-icon="mdi-account-supervisor">
          Owners ({{ props.subscription.owner.source }})
        </v-btn>
      </div>
      <div class="d-flex flex-row">
        <tooltip-icon
          v-if="!props.authorized"
          content="Sign in to edit the subscription"
        />
        <v-btn
          v-if="props.subscription.state === State.ACTIVE"
          :disabled="!props.authorized"
          color="orange"
          prepend-icon="mdi-publish-off"
        >
          Suspend
        </v-btn>
        <v-btn
          v-if="props.subscription.state === State.SUSPENDED"
          :disabled="!props.authorized"
          color="green"
          prepend-icon="mdi-publish"
        >
          Activate
        </v-btn>
        <v-btn :disabled="!props.authorized" prepend-icon="mdi-pencil">
          Edit
        </v-btn>
        <v-btn :disabled="!props.authorized" prepend-icon="mdi-content-copy">
          Clone
        </v-btn>
        <v-btn
          :disabled="!props.authorized"
          color="red"
          prepend-icon="mdi-delete"
        >
          Remove
        </v-btn>
      </div>
    </v-card-actions>
  </v-card>
</template>

<style scoped lang="scss">
  .subscription-header {
    &__actions {
      justify-content: space-between;
    }
  }
</style>
