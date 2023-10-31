<script setup lang="ts">
  import { computed } from 'vue';
  import { groupName } from '@/utils/topic-utils/topic-utils';
  import { parseSubscriptionFqn } from '@/utils/subscription-utils/subscription-utils';
  import { useFavorites } from '@/store/favorites/useFavorites';
  import { useI18n } from 'vue-i18n';
  import { useRouter } from 'vue-router';

  const router = useRouter();
  const { t } = useI18n();

  const favorites = useFavorites();

  const props = defineProps<{
    subscriptions?: string[];
    filter?: string;
  }>();

  const filteredSubscriptions = computed(() => {
    return (props.subscriptions ?? []).filter(
      (subscription) =>
        !props.filter ||
        subscription.toLowerCase().includes(props.filter.toLowerCase()),
    );
  });

  function onSubscriptionClick(subscriptionQualifiedName: string) {
    const { topicName, subscriptionName } = parseSubscriptionFqn(
      subscriptionQualifiedName,
    );
    router.push({
      path: `/ui/groups/${groupName(
        topicName,
      )}/topics/${topicName}/subscriptions/${subscriptionName}`,
    });
  }
</script>

<template>
  <v-card class="mb-2">
    <v-table density="comfortable" hover>
      <thead>
        <tr>
          <th>{{ t('favorites.subscriptions.index') }}</th>
          <th>{{ t('favorites.subscriptions.name') }}</th>
          <th></th>
        </tr>
      </thead>
      <tbody v-if="filteredSubscriptions.length > 0">
        <tr
          v-for="(subscription, index) in filteredSubscriptions"
          :key="subscription"
          class="subscriptions-table__row"
        >
          <td
            class="text-medium-emphasis"
            @click="onSubscriptionClick(subscription)"
          >
            {{ index + 1 }}
          </td>
          <td
            class="font-weight-medium"
            @click="onSubscriptionClick(subscription)"
          >
            {{ subscription }}
          </td>
          <td class="text-right" style="width: 50px">
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
                    favorites.removeSubscriptionByQualifiedName(subscription)
                  "
                />
              </template>
            </v-tooltip>
          </td>
        </tr>
      </tbody>
      <tbody v-else>
        <tr>
          <th colspan="3" class="text-center text-medium-emphasis">
            {{ t('search.results.subscription.noSubscriptions') }}
            <template v-if="filter">
              {{ t('favorites.subscriptions.appliedFilter', { filter }) }}
            </template>
          </th>
        </tr>
      </tbody>
    </v-table>
  </v-card>
</template>

<style scoped lang="scss">
  .subscriptions-table__row:hover {
    cursor: pointer;
  }
</style>
