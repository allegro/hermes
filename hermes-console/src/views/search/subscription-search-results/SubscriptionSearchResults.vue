<script setup lang="ts">
  import { groupName } from '@/utils/topic-utils/topic-utils';
  import { useRouter } from 'vue-router';
  import type { Subscription } from '@/api/subscription';

  const router = useRouter();

  const props = defineProps<{
    subscriptions: Subscription[];
  }>();

  function onSubscriptionClick(subscription: Subscription) {
    const group = groupName(subscription.topicName);
    router.push({
      path: `/ui/groups/${group}/topics/${subscription.topicName}/subscriptions/${subscription.name}`,
    });
  }
  function onSubscriptionBlankClick(subscription: Subscription) {
    const group = groupName(subscription.topicName);
    window.open(
      `/ui/groups/${group}/topics/${subscription.topicName}/subscriptions/${subscription.name}`,
      '_blank',
    );
  }
</script>

<template>
  <v-card class="mb-2">
    <v-table
      density="comfortable"
      data-testid="subscription-search-results"
      hover
    >
      <thead>
        <tr>
          <th>#</th>
          <th>{{ $t('search.results.subscription.name') }}</th>
          <th>{{ $t('search.results.subscription.endpoint') }}</th>
          <th>{{ $t('search.results.subscription.owner') }}</th>
          <th>{{ $t('search.results.subscription.status') }}</th>
          <th></th>
        </tr>
      </thead>
      <tbody v-if="props.subscriptions.length > 0">
        <tr
          v-for="(subscription, index) in subscriptions"
          :key="subscription.name"
          class="table__row"
          @click.exact="onSubscriptionClick(subscription)"
          @click.meta="onSubscriptionBlankClick(subscription)"
          @click.ctrl="onSubscriptionBlankClick(subscription)"
          @contextmenu="onSubscriptionBlankClick(subscription)"
        >
          <td class="text-medium-emphasis">
            {{ index + 1 }}
          </td>
          <td class="font-weight-medium">
            {{ subscription.name }}
          </td>
          <td class="font-weight-medium">
            {{ subscription.endpoint }}
          </td>
          <td class="font-weight-medium">
            {{ subscription.owner.id }}
          </td>
          <td class="font-weight-medium">
            {{ subscription.state }}
          </td>
          <td>
            <v-icon icon="mdi-chevron-right"></v-icon>
          </td>
        </tr>
      </tbody>
      <tbody v-else>
        <tr>
          <th colspan="12" class="text-center text-medium-emphasis">
            {{ $t('search.results.subscription.noSubscriptions') }}
          </th>
        </tr>
      </tbody>
    </v-table>
  </v-card>
</template>

<style scoped lang="scss">
  .table__row:hover {
    cursor: pointer;
  }
</style>
