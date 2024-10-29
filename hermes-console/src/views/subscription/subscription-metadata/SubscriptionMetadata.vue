<script setup lang="ts">
  import { copyToClipboard } from '@/utils/copy-utils';
  import { download } from '@/utils/download-utils';
  import { isAdmin, isSubscriptionOwnerOrAdmin } from '@/utils/roles-util';
  import { Owner } from '@/api/owner';
  import { ref } from 'vue';
  import { Role } from '@/api/role';
  import { State } from '@/api/subscription';
  import { subscriptionFqn } from '@/utils/subscription-utils/subscription-utils';
  import { useFavorites } from '@/store/favorites/useFavorites';
  import { useI18n } from 'vue-i18n';
  import { useRoute, useRouter } from 'vue-router';
  import SubscriptionForm from '@/views/subscription/subscription-form/SubscriptionForm.vue';
  import TooltipIcon from '@/components/tooltip-icon/TooltipIcon.vue';
  import type { Subscription } from '@/api/subscription';

  const { t } = useI18n();

  const favorites = useFavorites();

  const props = defineProps<{
    subscription: Subscription;
    owner: Owner;
    roles: Role[] | undefined;
  }>();

  const route = useRoute();

  const router = useRouter();

  const emit = defineEmits<{
    remove: [];
    suspend: [];
    activate: [];
  }>();

  const showSubscriptionEditForm = ref(false);
  function showSubscriptionForm() {
    showSubscriptionEditForm.value = true;
  }
  function hideSubscriptionForm() {
    showSubscriptionEditForm.value = false;
  }

  function refreshPage() {
    router.go(0);
  }

  function exportSubscription() {
    download(
      JSON.stringify(props.subscription),
      `${props.subscription.name}.json`,
      'application/json',
    );
  }
</script>

<template>
  <v-card density="compact">
    <div class="d-flex justify-end mr-4 mb-1">
      <v-dialog
        v-model="showSubscriptionEditForm"
        min-width="800"
        :persistent="true"
      >
        <v-card>
          <v-card-title>
            <span class="text-h5">
              {{
                t('subscription.subscriptionMetadata.editSubscription', {
                  subscriptionName: subscription.name,
                })
              }}
            </span>
          </v-card-title>
          <v-card-text>
            <SubscriptionForm
              operation="edit"
              :subscription="subscription"
              :topic="subscription.topicName"
              :roles="roles"
              @created="refreshPage"
              @cancel="hideSubscriptionForm"
            />
          </v-card-text>
        </v-card>
      </v-dialog>
    </div>
    <v-card-item>
      <p class="text-overline">
        {{ $t('subscription.subscriptionMetadata.subscription') }}
      </p>
      <div class="d-flex justify-space-between">
        <p class="text-h4 font-weight-bold">
          {{ props.subscription.name }}
          <v-chip
            :color="props.subscription.state === State.ACTIVE ? 'green' : 'red'"
            size="small"
          >
            {{ props.subscription.state }}
          </v-chip>
        </p>
        <div>
          <v-tooltip
            :text="$t('subscription.subscriptionMetadata.actions.copyName')"
          >
            <template v-slot:activator="{ props }">
              <v-btn
                icon="mdi-content-copy"
                variant="plain"
                v-bind="props"
                @click="
                  copyToClipboard(
                    subscriptionFqn(subscription.topicName, subscription.name),
                  )
                "
              />
            </template>
          </v-tooltip>
          <span
            v-if="
              favorites.subscriptions.includes(
                subscriptionFqn(subscription.topicName, subscription.name),
              )
            "
          >
            <v-tooltip
              :text="
                $t(
                  'subscription.subscriptionMetadata.actions.removeFromFavorites',
                )
              "
            >
              <template v-slot:activator="{ props }">
                <v-btn
                  icon="mdi-star"
                  variant="plain"
                  v-bind="props"
                  @click="
                    favorites.removeSubscription(
                      subscription.topicName,
                      subscription.name,
                    )
                  "
                />
              </template>
            </v-tooltip>
          </span>
          <span
            v-if="
              !favorites.subscriptions.includes(
                subscriptionFqn(subscription.topicName, subscription.name),
              )
            "
          >
            <v-tooltip
              :text="
                $t('subscription.subscriptionMetadata.actions.addToFavorites')
              "
            >
              <template v-slot:activator="{ props }">
                <v-btn
                  icon="mdi-star-outline"
                  variant="plain"
                  v-bind="props"
                  @click="
                    favorites.addSubscription(
                      subscription.topicName,
                      subscription.name,
                    )
                  "
                />
              </template>
            </v-tooltip>
          </span>
        </div>
      </div>

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
          @click="emit('suspend')"
        >
          {{ $t('subscription.subscriptionMetadata.actions.suspend') }}
        </v-btn>
        <v-btn
          v-if="props.subscription.state === State.SUSPENDED"
          :disabled="!isSubscriptionOwnerOrAdmin(roles)"
          color="green"
          prepend-icon="mdi-publish"
          @click="emit('activate')"
        >
          {{ $t('subscription.subscriptionMetadata.actions.activate') }}
        </v-btn>
        <v-btn
          :disabled="!isSubscriptionOwnerOrAdmin(roles)"
          prepend-icon="mdi-pencil"
          @click="showSubscriptionForm"
        >
          {{ $t('subscription.subscriptionMetadata.actions.edit') }}
        </v-btn>
        <v-btn
          :disabled="!isSubscriptionOwnerOrAdmin(roles)"
          prepend-icon="mdi-export"
          @click="exportSubscription"
        >
          {{ $t('subscription.subscriptionMetadata.actions.export') }}
        </v-btn>
        <v-btn
          :disabled="!isSubscriptionOwnerOrAdmin(roles)"
          color="red"
          prepend-icon="mdi-delete"
          @click="emit('remove')"
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
