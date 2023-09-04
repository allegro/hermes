<script setup lang="ts">
  import { copyToClipboard } from '@/utils/copy-utils';
  import {
    isSubscriptionOwnerOrAdmin,
    isTopicOwnerOrAdmin,
  } from '@/utils/roles-util';
  import { useAppConfigStore } from '@/store/app-config/useAppConfigStore';
  import { useFavorites } from '@/store/favorites/useFavorites';
  import TooltipIcon from '@/components/tooltip-icon/TooltipIcon.vue';
  import type { Owner } from '@/api/owner';
  import type { Role } from '@/api/role';
  import type { TopicWithSchema } from '@/api/topic';

  const favorites = useFavorites();

  const props = defineProps<{
    topic: TopicWithSchema;
    owner: Owner;
    roles: Role[] | undefined;
  }>();

  const configStore = useAppConfigStore();

  const emit = defineEmits<{
    remove: [];
  }>();
</script>

<template>
  <v-card density="compact">
    <v-card-item>
      <p class="text-overline">{{ $t('topicView.header.topic') }}</p>
      <div class="d-flex justify-space-between">
        <p class="text-h4 font-weight-bold">{{ props.topic.name }}</p>
        <div>
          <v-tooltip :text="$t('topicView.header.actions.copyName')">
            <template v-slot:activator="{ props }">
              <v-btn
                icon="mdi-content-copy"
                variant="plain"
                v-bind="props"
                @click="copyToClipboard(topic.name)"
              />
            </template>
          </v-tooltip>
          <span v-if="favorites.topics.includes(topic.name)">
            <v-tooltip
              :text="$t('topicView.header.actions.removeFromFavorites')"
            >
              <template v-slot:activator="{ props }">
                <v-btn
                  icon="mdi-star"
                  variant="plain"
                  v-bind="props"
                  @click="favorites.removeTopic(topic.name)"
                />
              </template>
            </v-tooltip>
          </span>
          <span v-if="!favorites.topics.includes(topic.name)">
            <v-tooltip :text="$t('topicView.header.actions.addToFavorites')">
              <template v-slot:activator="{ props }">
                <v-btn
                  icon="mdi-star-outline"
                  variant="plain"
                  v-bind="props"
                  @click="favorites.addTopic(topic.name)"
                />
              </template>
            </v-tooltip>
          </span>
        </div>
      </div>
    </v-card-item>

    <v-card-text>
      <p class="text-subtitle-2">{{ props.topic.description }}</p>
    </v-card-text>

    <v-divider class="mx-4 mb-1" />

    <v-card-actions class="d-flex justify-space-between">
      <div>
        <v-btn
          class="text-none"
          prepend-icon="mdi-account-supervisor"
          :href="props.owner.url"
          target="_blank"
          color="blue"
        >
          {{ $t('topicView.header.owner') }} {{ props.owner.name }}
        </v-btn>
      </div>
      <div class="d-flex flex-row">
        <tooltip-icon
          v-if="!isSubscriptionOwnerOrAdmin(roles)"
          :content="$t('topicView.header.unauthorizedTooltip')"
        />
        <v-btn
          :disabled="
            configStore.loadedConfig.topic.readOnlyModeEnabled ||
            !isTopicOwnerOrAdmin(roles)
          "
          prepend-icon="mdi-pencil"
          >{{ $t('topicView.header.actions.edit') }}
        </v-btn>
        <v-btn
          prepend-icon="mdi-content-copy"
          :disabled="!isTopicOwnerOrAdmin(roles)"
          >{{ $t('topicView.header.actions.clone') }}
        </v-btn>
        <v-btn
          v-if="
            configStore.loadedConfig.topic.offlineRetransmissionEnabled &&
            topic.offlineStorage.enabled &&
            isTopicOwnerOrAdmin(roles)
          "
          prepend-icon="mdi-transmission-tower"
          >{{ $t('topicView.header.actions.offlineRetransmission') }}
        </v-btn>
        <v-btn
          color="red"
          prepend-icon="mdi-delete"
          @click="emit('remove')"
          :disabled="!isTopicOwnerOrAdmin(roles)"
          >{{ $t('topicView.header.actions.remove') }}
        </v-btn>
      </div>
    </v-card-actions>
  </v-card>
</template>

<style scoped lang="scss"></style>
