<script setup lang="ts">
  import { formatTimestampMillis } from '@/utils/date-formatter/date-formatter';
  import { ref } from 'vue';
  import { useI18n } from 'vue-i18n';
  import MessagePreviewDialog from '@/views/topic/messages-preview/message-preview-dialog/MessagePreviewDialog.vue';
  import type {
    MessagePartiallyParsedContent,
    ParsedMessagePreview,
    SelectedRow,
  } from '@/views/topic/messages-preview/types';
  import type { MessagePreview } from '@/api/topic';

  const props = defineProps<{
    messages: MessagePreview[];
    enabled: boolean;
  }>();

  const { t } = useI18n();

  const parsedMessages: ParsedMessagePreview[] = props.messages.map(
    (message) => {
      let parsedContent: MessagePartiallyParsedContent | null = null;
      try {
        if (!message.truncated) {
          parsedContent = JSON.parse(message.content || '{}');
        }
      } catch (e) {
        /* empty */
      }

      return {
        key: message,
        ...message,
        messageId: parsedContent?.__metadata?.messageId || null,
        timestamp: parsedContent?.__metadata?.timestamp
          ? Number(parsedContent?.__metadata?.timestamp)
          : null,
        parsedContent,
      };
    },
  );

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
        :items="enabled ? parsedMessages : []"
        :headers="messagesTableHeaders"
        hover
        @click:row="
          (_: Event, row: SelectedRow<ParsedMessagePreview>) =>
            openMessageDialog(row.item)
        "
        :no-data-text="
          enabled
            ? $t('topicView.messagesPreview.messageDetails.noMessages')
            : $t('topicView.messagesPreview.messageDetails.disabled')
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
    <message-preview-dialog
      :message="selectedMessage"
      @close="closeMessageDialog"
    />
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
    max-height: 70px;
    white-space: wrap;
    display: -webkit-box;
    -webkit-line-clamp: 3;
    line-clamp: 3;
    -webkit-box-orient: vertical;
    overflow: hidden;
    word-wrap: break-word;
    text-overflow: ellipsis;
    margin: 4px 0;
  }
</style>
