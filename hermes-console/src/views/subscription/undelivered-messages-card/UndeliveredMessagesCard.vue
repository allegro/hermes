<script setup lang="ts">
  import { formatTimestampMillis } from '@/utils/date-formatter/date-formatter';
  import type { SentMessageTrace } from '@/api/subscription-undelivered';

  const props = defineProps<{
    undeliveredMessages?: SentMessageTrace[];
  }>();
</script>

<template>
  <v-card>
    <v-card-item class="border-b">
      <v-card-title class="font-weight-bold"
        >{{ $t('subscription.undeliveredMessagesCard.title') }}
      </v-card-title>
    </v-card-item>

    <v-card-item>
      <v-table density="compact">
        <thead>
          <tr>
            <th class="text-left pl-0">
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
          </tr>
        </thead>
        <tbody
          v-if="
            props.undeliveredMessages && props.undeliveredMessages.length > 0
          "
        >
          <tr
            v-for="(message, index) in props.undeliveredMessages.slice(0, 100)"
            :key="index"
          >
            <td class="pl-0">{{ index + 1 }}</td>
            <td>{{ message.messageId }}</td>
            <td>{{ message.status }}</td>
            <td>{{ message.reason }}</td>
            <td>{{ formatTimestampMillis(message.timestamp) }}</td>
          </tr>
        </tbody>
        <tbody v-else>
          <tr>
            <td
              colspan="5"
              class="text-center text-medium-emphasis text-body-2"
            >
              <span>{{
                $t('subscription.undeliveredMessagesCard.noUndeliveredMessages')
              }}</span>
            </td>
          </tr>
        </tbody>
      </v-table>
    </v-card-item>
  </v-card>
</template>

<style scoped lang="scss"></style>
