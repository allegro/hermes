<script setup lang="ts">
  import { formatTimestampMillis } from '@/utils/date-formatter/date-formatter';
  import { ref } from 'vue';
  import { useI18n } from 'vue-i18n';
  import JsonViewer from '@/components/json-viewer/JsonViewer.vue';
  import type { MessagePreview } from '@/api/topic';
  import type {
    ParsedMessagePreview,
    SelectedRow,
  } from '@/views/topic/messages-preview/types';

  const props = defineProps<{
    messages: MessagePreview[];
  }>();

  const { t } = useI18n();

  const parsedMessages = props.messages.map((message) => {
    let parsedContent: any | null = null;
    try {
      if (!message.truncated) {
        parsedContent = JSON.parse(message.content || '{}');
      }
    } catch (e) {
      /* empty */
    }

    return {
      ...message,
      messageId: parsedContent?.__metadata?.messageId || 'Not available',
      timestamp: parsedContent?.__metadata?.timestamp
        ? Number(parsedContent?.__metadata?.timestamp)
        : null,
      parsedContent,
    };
  });

  const messagesTableHeaders = [
    {
      title: t('topicView.messagesPreview.tableHeaders.messageId'),
      key: 'messageId',
    },
    {
      title: t('topicView.messagesPreview.tableHeaders.timestamp'),
      key: 'timestamp',
    },
    {
      title: t('topicView.messagesPreview.tableHeaders.content'),
      key: 'content',
    },
    {
      title: t('topicView.messagesPreview.tableHeaders.truncated'),
      key: 'truncated',
    },
  ];

  const selectedMessage = ref<ParsedMessagePreview | null>(null);
  const dialog = ref(false);

  function openMessageDialog(message: ParsedMessagePreview) {
    selectedMessage.value = message;
    dialog.value = true;
    console.log(message);
  }

  function closeMessageDialog() {
    dialog.value = false;
    selectedMessage.value = null;
  }
</script>

<template>
  <v-card>
    <template #title>
      <div class="d-flex justify-space-between">
        <p class="font-weight-bold">
          {{ $t('topicView.messagesPreview.title') }}
        </p>
      </div>
    </template>
    <v-card-text>
      <v-data-table
        :items="parsedMessages"
        :headers="messagesTableHeaders"
        hover
        @click:row="
          (_: Event, row: SelectedRow<ParsedMessagePreview>) =>
            openMessageDialog(row.item)
        "
      >
        <template #[`item.messageId`]="{ item }">
          {{
            item?.messageId ||
            $t('topicView.messagesPreview.messageDetails.notAvailable')
          }}
        </template>
        <template #[`item.timestamp`]="{ item }">
          {{
            item?.timestamp
              ? formatTimestampMillis(item?.timestamp)
              : $t('topicView.messagesPreview.messageDetails.notAvailable')
          }}
        </template>
        <template #[`item.content`]="{ item }">
          <span class="table-row-content-cell">{{ item.content }}</span>
        </template>
      </v-data-table>
    </v-card-text>
  </v-card>

  <v-dialog
    v-model="dialog"
    min-width="800"
    max-height="800"
    v-if="selectedMessage"
  >
    <v-card>
      <v-card-item class="border-b">
        <div class="d-flex justify-space-between align-start">
          <div>
            <v-card-title>{{
              $t('topicView.messagesPreview.messageDetails.title')
            }}</v-card-title>
            <v-card-subtitle>
              {{ $t('topicView.messagesPreview.messageDetails.subtitle') }}
            </v-card-subtitle>
          </div>
          <v-btn icon="mdi-close" variant="text" @click="closeMessageDialog" />
        </div>
      </v-card-item>

      <v-card-text class="pt-4 d-flex flex-column row-gap-4">
        <div class="d-flex flex-column row-gap-1">
          <span class="text-body-2 text-medium-emphasis">{{
            $t('topicView.messagesPreview.messageDetails.messageId')
          }}</span>
          <span>{{
            selectedMessage.messageId ||
            $t('topicView.messagesPreview.messageDetails.notAvailable')
          }}</span>
        </div>
        <div class="d-flex flex-column row-gap-1">
          <span class="text-body-2 text-medium-emphasis">{{
            $t('topicView.messagesPreview.messageDetails.timestamp')
          }}</span>
          <span>{{
            selectedMessage?.timestamp
              ? formatTimestampMillis(selectedMessage.timestamp)
              : $t('topicView.messagesPreview.messageDetails.notAvailable')
          }}</span>
        </div>
        <div class="d-flex flex-column row-gap-1">
          <span class="text-body-2 text-medium-emphasis">{{
            $t('topicView.messagesPreview.messageDetails.content')
          }}</span>
          <json-viewer
            v-if="selectedMessage.parsedContent"
            :json="selectedMessage.content"
            class="border-thin rounded-lg"
          />
          <div class="raw-schema-snippet pa-3 bg-grey-lighten-5" v-else>
            {{ selectedMessage.content }}
          </div>
        </div>
      </v-card-text>
    </v-card>
  </v-dialog>
</template>

<style scoped lang="scss">
  @use 'vuetify/settings';

  .raw-schema-snippet {
    line-height: 1.2;
    font-size: 0.9rem;
    max-height: 500px;
    overflow: auto;
    cursor: text;
    border: 1px rgba(var(--v-border-color), var(--v-border-opacity)) solid;
    border-radius: settings.$border-radius-root;
  }

  .table-row-content-cell {
    display: block;
    max-height: 70px;
    white-space: wrap;
    display: -webkit-box;
    -webkit-line-clamp: 3;
    line-clamp: 3;
    -webkit-box-orient: vertical;
    overflow: hidden;
    word-wrap: break-word;
    text-overflow: ellipsis;
  }
</style>
