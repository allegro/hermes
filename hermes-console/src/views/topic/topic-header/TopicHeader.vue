<script setup lang="ts">
  import { copyToClipboard } from '@/utils/copy-utils';
  import { download } from '@/utils/download-utils';
  import {
    isSubscriptionOwnerOrAdmin,
    isTopicOwnerOrAdmin,
  } from '@/utils/roles-util';
  import { ref } from 'vue';
  import { useAppConfigStore } from '@/store/app-config/useAppConfigStore';
  import { useFavorites } from '@/store/favorites/useFavorites';
  import { useI18n } from 'vue-i18n';
  import { useOfflineRetransmission } from '@/composables/topic/use-offline-retransmission/useOfflineRetransmission';
  import { useRouter } from 'vue-router';
  import OfflineRetransmissionDialog from '@/views/topic/offline-retransmission/OfflineRetransmissionDialog.vue';
  import TooltipIcon from '@/components/tooltip-icon/TooltipIcon.vue';
  import TopicForm from '@/views/topic/topic-form/TopicForm.vue';
  import type { Owner } from '@/api/owner';
  import type { Role } from '@/api/role';
  import type { TopicWithSchema } from '@/api/topic';

  const TOPIC_RETRANSMISSION = 'topic';
  const favorites = useFavorites();

  const router = useRouter();

  const { t } = useI18n();

  const props = defineProps<{
    topic: TopicWithSchema;
    owner: Owner;
    roles: Role[] | undefined;
  }>();

  const configStore = useAppConfigStore();

  const emit = defineEmits<{
    remove: [];
  }>();

  const offlineRetransmission = useOfflineRetransmission();

  const onRetransmit = (
    targetTopic: string,
    startTimestamp: string,
    endTimestamp: string,
  ) => {
    offlineRetransmission.retransmit({
      type: TOPIC_RETRANSMISSION,
      sourceTopic: props.topic.name,
      targetTopic,
      startTimestamp,
      endTimestamp,
    });
  };

  const showTopicEditForm = ref(false);
  function showTopicForm() {
    showTopicEditForm.value = true;
  }
  function hideTopicForm() {
    showTopicEditForm.value = false;
  }

  function refreshPage() {
    router.go(0);
  }

  function exportTopic() {
    download(
      JSON.stringify(props.topic),
      `${props.topic.name.replace('.', '_')}.json`,
      'application/json',
    );
  }
</script>

<template>
  <v-card density="compact">
    <div class="d-flex justify-end mr-4 mb-1">
      <v-dialog v-model="showTopicEditForm" min-width="800" :persistent="true">
        <v-card>
          <v-card-title>
            <span class="text-h5">
              {{
                t('topicView.header.editTopic', {
                  topicName: topic.name,
                })
              }}
            </span>
          </v-card-title>
          <v-card-text>
            <TopicForm
              operation="edit"
              :topic="topic"
              :group="null"
              :roles="roles"
              @created="refreshPage"
              @cancel="hideTopicForm"
            />
          </v-card-text>
        </v-card>
      </v-dialog>
    </div>
    <v-card-item>
      <p class="text-overline">{{ $t('topicView.header.topic') }}</p>
      <div class="d-flex justify-space-between">
        <p
          class="text-h4 font-weight-bold"
          style="word-wrap: break-word; max-width: 90%"
        >
          {{ props.topic.name }}
        </p>
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
          @click="showTopicForm"
          >{{ $t('topicView.header.actions.edit') }}
        </v-btn>
        <v-btn
          prepend-icon="mdi-export"
          :disabled="!isTopicOwnerOrAdmin(roles)"
          @click="exportTopic"
          >{{ $t('topicView.header.actions.export') }}
        </v-btn>
        <v-btn
          v-if="
            configStore.loadedConfig.topic.offlineRetransmissionEnabled &&
            topic.offlineStorage.enabled &&
            isTopicOwnerOrAdmin(roles)
          "
          prepend-icon="mdi-transmission-tower"
          data-testid="offlineRetransmissionButton"
          >{{ $t('topicView.header.actions.offlineRetransmission') }}
          <OfflineRetransmissionDialog @retransmit="onRetransmit">
          </OfflineRetransmissionDialog>
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
