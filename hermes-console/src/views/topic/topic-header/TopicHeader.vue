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
  import { useRouter } from 'vue-router';
  import TooltipIcon from '@/components/tooltip-icon/TooltipIcon.vue';
  import TopicForm from '@/views/topic/topic-form/TopicForm.vue';
  import type { Owner } from '@/api/owner';
  import type { Role } from '@/api/role';
  import type { TopicWithSchema } from '@/api/topic';

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
  <v-card density="compact" color="transparent" border="false">
    <div class="d-flex justify-end mr-4 mb-1">
      <v-dialog v-model="showTopicEditForm" min-width="800" :persistent="true">
        <v-card>
          <v-card-item class="border-b">
            <div class="d-flex justify-space-between align-center">
              <v-card-title>
                {{
                  t('topicView.header.editTopic', {
                    topicName: topic.name,
                  })
                }}
              </v-card-title>
              <v-btn icon="mdi-close" variant="text" @click="hideTopicForm" />
            </div>
          </v-card-item>
          <v-card-text class="pt-4">
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
      <p class="text-overline text-primary">
        {{ $t('topicView.header.topic') }}
      </p>
      <div class="d-flex justify-space-between">
        <div class="d-flex flex-grow-1 column-gap-1">
          <p
            class="text-h4 font-weight-bold"
            style="word-wrap: break-word; max-width: 90%"
          >
            {{ props.topic.name }}
          </p>
          <tooltip-icon
            v-if="!isSubscriptionOwnerOrAdmin(roles)"
            :content="$t('topicView.header.unauthorizedTooltip')"
          />
        </div>
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

    <v-card-text class="d-flex justify-space-between">
      <div>
        <p class="text-body-1 text-medium-emphasis">
          {{ props.topic.description }}
        </p>
        <p class="text-body-2 text-medium-emphasis">
          <v-icon>mdi-account-supervisor</v-icon>
          {{ $t('topicView.header.owner') }}
          <a
            :href="props.owner.url"
            target="_blank"
            class="text-decoration-none text-button text-none text-primary"
            >{{ props.owner.name }}</a
          >
        </p>
      </div>
      <div class="d-flex column-gap-2">
        <v-btn
          prepend-icon="mdi-export"
          class="text-capitalize"
          variant="outlined"
          @click="exportTopic"
          >{{ $t('topicView.header.actions.export') }}
        </v-btn>

        <v-btn
          :disabled="
            configStore.loadedConfig.topic.readOnlyModeEnabled ||
            !isTopicOwnerOrAdmin(roles)
          "
          prepend-icon="mdi-pencil"
          class="text-capitalize"
          @click="showTopicForm"
          color="primary"
          >{{ $t('topicView.header.actions.edit') }}
        </v-btn>

        <v-btn
          color="error"
          prepend-icon="mdi-delete"
          class="text-capitalize"
          @click="emit('remove')"
          :disabled="!isTopicOwnerOrAdmin(roles)"
          >{{ $t('topicView.header.actions.remove') }}
        </v-btn>
      </div>
    </v-card-text>
  </v-card>
</template>

<style scoped lang="scss"></style>
