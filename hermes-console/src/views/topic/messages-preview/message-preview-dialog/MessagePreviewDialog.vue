<script setup lang="ts">
  import { formatTimestampMillis } from '@/utils/date-formatter/date-formatter';
  import JsonViewer from '@/components/json-viewer/JsonViewer.vue';
  import type { ParsedMessagePreview } from '@/views/topic/messages-preview/types';

  const props = defineProps<{
    message: ParsedMessagePreview;
  }>();

  const emit = defineEmits(['close']);
</script>

<template>
  <v-card>
    <v-card-item class="border-b">
      <div class="d-flex justify-space-between align-start">
        <div>
          <v-card-title
            >{{ $t('topicView.messagesPreview.messageDetails.title') }}
          </v-card-title>
          <v-card-subtitle>
            {{ $t('topicView.messagesPreview.messageDetails.subtitle') }}
          </v-card-subtitle>
        </div>
        <v-btn
          icon="mdi-close"
          variant="text"
          @click="emit('close')"
          data-testid="close-button"
        />
      </div>
    </v-card-item>

    <v-card-text class="pt-4 d-flex flex-column row-gap-4">
      <div class="d-flex flex-column row-gap-1">
        <span class="text-body-2 text-medium-emphasis">{{
          $t('topicView.messagesPreview.messageDetails.messageId')
        }}</span>
        <span>{{
          props.message.messageId ||
          $t('topicView.messagesPreview.messageDetails.notAvailable')
        }}</span>
      </div>
      <div class="d-flex flex-column row-gap-1">
        <span class="text-body-2 text-medium-emphasis">{{
          $t('topicView.messagesPreview.messageDetails.timestamp')
        }}</span>
        <span>{{
          props.message?.timestamp
            ? formatTimestampMillis(props.message.timestamp)
            : $t('topicView.messagesPreview.messageDetails.notAvailable')
        }}</span>
      </div>
      <div class="d-flex flex-column row-gap-1">
        <span class="text-body-2 text-medium-emphasis">{{
          $t('topicView.messagesPreview.messageDetails.content')
        }}</span>
        <json-viewer
          v-if="props.message.parsedContent"
          :json="props.message.content"
          class="border-thin rounded-lg"
          data-testid="json-viewer"
        />
        <div
          class="raw-schema-snippet rounded-lg pa-3 raw-message-content"
          v-else
        >
          {{ props.message.content }}
        </div>
      </div>
    </v-card-text>
  </v-card>
</template>

<style scoped lang="scss">
  @use 'vuetify/settings';

  .raw-message-content {
    font-size: settings.$code-font-size;
    line-height: 1.2;
    max-height: 500px;
    overflow: auto;
    cursor: text;
    color: settings.$code-color;
    background-color: settings.$code-background-color;
  }
</style>
