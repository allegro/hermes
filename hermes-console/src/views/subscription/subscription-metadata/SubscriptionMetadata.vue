<script setup lang="ts">
  import { copyToClipboard } from '@/utils/copy-utils';
  import { download } from '@/utils/download-utils';
  import { getAvroPaths } from '@/utils/json-avro/jsonAvroUtils';
  import { isAdmin, isSubscriptionOwnerOrAdmin } from '@/utils/roles-util';
  import { ref } from 'vue';
  import { State } from '@/api/subscription';
  import { subscriptionFqn } from '@/utils/subscription-utils/subscription-utils';
  import { useFavorites } from '@/store/favorites/useFavorites';
  import { useI18n } from 'vue-i18n';
  import { useRoute, useRouter } from 'vue-router';
  import SubscriptionForm from '@/views/subscription/subscription-form/SubscriptionForm.vue';
  import TooltipIcon from '@/components/tooltip-icon/TooltipIcon.vue';
  import type { Owner } from '@/api/owner';
  import type { Role } from '@/api/role';
  import type { Subscription } from '@/api/subscription';

  const { t } = useI18n();

  const favorites = useFavorites();

  const props = defineProps<{
    subscription: Subscription;
    owner: Owner;
    roles: Role[] | undefined;
    schema?: string;
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
  <v-card density="compact" color="transparent" border="false">
    <div class="d-flex justify-end mr-4 mb-1">
      <v-dialog
        v-model="showSubscriptionEditForm"
        min-width="800"
        :persistent="true"
      >
        <v-card>
          <v-card-item class="border-b">
            <div class="d-flex justify-space-between align-center">
              <v-card-title>
                {{
                  t('subscription.subscriptionMetadata.editSubscription', {
                    subscriptionName: props.subscription.name,
                  })
                }}
              </v-card-title>
              <v-btn
                icon="mdi-close"
                variant="text"
                @click="hideSubscriptionForm"
              />
            </div>
          </v-card-item>

          <v-card-text class="pt-4">
            <SubscriptionForm
              operation="edit"
              :subscription="props.subscription"
              :topic="props.subscription.topicName"
              :roles="props.roles"
              :paths="getAvroPaths(props.schema)"
              @created="refreshPage"
              @cancel="hideSubscriptionForm"
            />
          </v-card-text>
        </v-card>
      </v-dialog>
    </div>
    <v-card-item>
      <p class="text-overline text-primary">
        {{ $t('subscription.subscriptionMetadata.subscription') }}
      </p>
      <div class="d-flex justify-space-between">
        <div class="d-flex flex-grow-1 column-gap-1">
          <div class="d-flex flex-grow-1 column-gap-1">
            <p class="text-h4 font-weight-bold">
              {{ props.subscription.name }}
            </p>
            <tooltip-icon
              v-if="!isSubscriptionOwnerOrAdmin(props.roles)"
              :content="
                $t('subscription.subscriptionMetadata.unauthorizedTooltip')
              "
            />
            <v-chip
              :color="
                props.subscription.state === State.ACTIVE ? 'green' : 'error'
              "
              size="small"
              class="align-self-center"
            >
              {{ props.subscription.state }}
            </v-chip>
          </div>
        </div>
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
                    subscriptionFqn(
                      props.subscription.topicName,
                      props.subscription.name,
                    ),
                  )
                "
              />
            </template>
          </v-tooltip>
          <span
            v-if="
              favorites.subscriptions.includes(
                subscriptionFqn(
                  props.subscription.topicName,
                  props.subscription.name,
                ),
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
                      props.subscription.topicName,
                      props.subscription.name,
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
                      props.subscription.topicName,
                      props.subscription.name,
                    )
                  "
                />
              </template>
            </v-tooltip>
          </span>
        </div>
      </div>
    </v-card-item>

    <v-card-text class="d-flex justify-space-between">
      <div>
        <p class="text-body-1 text-medium-emphasis">
          {{ props.subscription.description }}
        </p>
        <p class="text-body-2 text-medium-emphasis">
          <v-icon>mdi-account-supervisor</v-icon>
          {{ $t('subscription.subscriptionMetadata.owners') }}
          <a
            :href="props.owner.url"
            target="_blank"
            class="text-decoration-none text-button text-none text-primary"
            >{{ props.owner.name }}</a
          >
        </p>
        <p class="text-body-2 text-medium-emphasis">
          <v-icon>mdi-api</v-icon>
          {{ $t('subscription.subscriptionMetadata.endpoint') }}
          {{ props.subscription.endpoint }}
        </p>
      </div>
      <div class="d-flex column-gap-2">
        <v-btn
          v-if="isAdmin(props.roles)"
          :to="`${route.path}/diagnostics`"
          prepend-icon="mdi-doctor"
          class="text-capitalize"
          variant="outlined"
        >
          {{ $t('subscription.subscriptionMetadata.actions.diagnostics') }}
        </v-btn>
        <v-btn
          :disabled="!isSubscriptionOwnerOrAdmin(props.roles)"
          prepend-icon="mdi-pencil"
          @click="showSubscriptionForm"
          class="text-capitalize"
          variant="outlined"
        >
          {{ $t('subscription.subscriptionMetadata.actions.edit') }}
        </v-btn>
        <v-btn
          prepend-icon="mdi-export"
          @click="exportSubscription"
          class="text-capitalize"
          variant="outlined"
        >
          {{ $t('subscription.subscriptionMetadata.actions.export') }}
        </v-btn>
        <v-btn
          v-if="props.subscription.state === State.SUSPENDED"
          :disabled="!isSubscriptionOwnerOrAdmin(props.roles)"
          color="green"
          prepend-icon="mdi-publish"
          @click="emit('activate')"
          class="text-capitalize"
        >
          {{ $t('subscription.subscriptionMetadata.actions.activate') }}
        </v-btn>
        <v-btn
          v-if="props.subscription.state === State.ACTIVE"
          :disabled="!isSubscriptionOwnerOrAdmin(props.roles)"
          color="orange"
          prepend-icon="mdi-publish-off"
          @click="emit('suspend')"
          class="text-capitalize"
        >
          {{ $t('subscription.subscriptionMetadata.actions.suspend') }}
        </v-btn>
        <v-btn
          :disabled="!isSubscriptionOwnerOrAdmin(props.roles)"
          color="error"
          prepend-icon="mdi-delete"
          @click="emit('remove')"
          class="text-capitalize"
        >
          {{ $t('subscription.subscriptionMetadata.actions.remove') }}
        </v-btn>
      </div>
    </v-card-text>
  </v-card>
</template>

<style scoped lang="scss">
  .subscription-header {
    &__actions {
      justify-content: space-between;
    }
  }
</style>
