<script setup lang="ts">
  import { isAdmin, isSubscriptionOwnerOrAdmin } from '@/utils/roles-util';
  import { Owner } from '@/api/owner';
  import { Role } from '@/api/role';
  import { State } from '@/api/subscription';
  import { useRoute } from 'vue-router';
  import TooltipIcon from '@/components/tooltip-icon/TooltipIcon.vue';
  import type { Subscription } from '@/api/subscription';

  const props = defineProps<{
    subscription: Subscription;
    owner: Owner;
    roles: Role[] | undefined;
  }>();

  const route = useRoute();

  defineEmits(['remove']);
</script>

<template>
  <v-card density="compact">
    <v-card-item>
      <p class="text-overline">
        {{ $t('subscription.subscriptionMetadata.subscription') }}
      </p>
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
        <v-btn
          class="text-none"
          prepend-icon="mdi-account-supervisor"
          :href="props.owner.url"
          target="_blank"
          color="blue"
        >
          {{ $t('subscription.subscriptionMetadata.owners') }}
          {{ props.owner.name }}
        </v-btn>
      </div>
      <div class="d-flex flex-row">
        <tooltip-icon
          v-if="!isSubscriptionOwnerOrAdmin(roles)"
          :content="$t('subscription.subscriptionMetadata.unauthorizedTooltip')"
        />
        <v-btn
          v-if="isAdmin(roles)"
          :to="`${route.path}/diagnostics`"
          color="green"
          prepend-icon="mdi-doctor"
        >
          {{ $t('subscription.subscriptionMetadata.actions.diagnostics') }}
        </v-btn>
        <v-btn
          v-if="props.subscription.state === State.ACTIVE"
          :disabled="!isSubscriptionOwnerOrAdmin(roles)"
          color="orange"
          prepend-icon="mdi-publish-off"
        >
          {{ $t('subscription.subscriptionMetadata.actions.suspend') }}
        </v-btn>
        <v-btn
          v-if="props.subscription.state === State.SUSPENDED"
          :disabled="!isSubscriptionOwnerOrAdmin(roles)"
          color="green"
          prepend-icon="mdi-publish"
        >
          {{ $t('subscription.subscriptionMetadata.actions.activate') }}
        </v-btn>
        <v-btn
          :disabled="!isSubscriptionOwnerOrAdmin(roles)"
          prepend-icon="mdi-pencil"
        >
          {{ $t('subscription.subscriptionMetadata.actions.edit') }}
        </v-btn>
        <v-btn
          :disabled="!isSubscriptionOwnerOrAdmin(roles)"
          prepend-icon="mdi-content-copy"
        >
          {{ $t('subscription.subscriptionMetadata.actions.clone') }}
        </v-btn>
        <v-btn
          :disabled="!isSubscriptionOwnerOrAdmin(roles)"
          color="red"
          prepend-icon="mdi-delete"
          @click="$emit('remove')"
        >
          {{ $t('subscription.subscriptionMetadata.actions.remove') }}
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
