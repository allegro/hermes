<script setup lang="ts">
  import { formatTimestamp } from '@/utils/date-formatter/date-formatter';
  import type { SentMessageTrace } from '@/api/subscription-undelivered';

  const props = defineProps<{
    undeliveredMessages: SentMessageTrace[];
  }>();
</script>

<template>
  <v-card class="mb-2">
    <template #title>
      <p class="font-weight-bold">
        {{ $t('subscription.undeliveredMessagesCard.title') }}
      </p>
    </template>
    <v-table density="compact">
      <thead>
        <tr>
          <th class="text-left">
            {{ $t('subscription.undeliveredMessagesCard.index') }}
          </th>
          <th class="text-left">
            {{ $t('subscription.undeliveredMessagesCard.messageId') }}
          </th>
          <th class="text-left">
            {{ $t('subscription.undeliveredMessagesCard.status') }}
          </th>
          <th class="text-left">
            {{ $t('subscription.undeliveredMessagesCard.reason') }}
          </th>
          <th class="text-left">
            {{ $t('subscription.undeliveredMessagesCard.timestamp') }}
          </th>
          <th class="text-left"></th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="(message, index) in props.undeliveredMessages.slice(0, 100)"
          :key="index"
        >
          <td>{{ index + 1 }}</td>
          <td>{{ message.messageId }}</td>
          <td>{{ message.status }}</td>
          <td>{{ message.reason }}</td>
          <td>{{ formatTimestamp(message.timestamp) }}</td>
          <td>
            <v-btn
              density="compact"
              icon="mdi-magnify"
              size="small"
              variant="flat"
            />
          </td>
        </tr>
      </tbody>
    </v-table>
  </v-card>
</template>

<style scoped lang="scss"></style>
