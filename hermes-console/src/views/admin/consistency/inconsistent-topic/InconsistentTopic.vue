<script setup lang="ts">
  import { parseSubscriptionFqn } from '@/utils/subscription-utils/subscription-utils';
  import { useConsistencyStore } from '@/store/consistency/useConsistencyStore';
  import { useI18n } from 'vue-i18n';
  import { useRoute, useRouter } from 'vue-router';
  import { useSync } from '@/composables/sync/use-sync/useSync';
  import InconsistentMetadata from '@/views/admin/consistency/inconsistent-metadata/InconsistentMetadata.vue';

  const router = useRouter();
  const route = useRoute();
  const { t } = useI18n();

  const { groupId, topicId } = route.params as Record<string, string>;

  const consistencyStore = useConsistencyStore();
  const { syncTopic, syncSubscription } = useSync();

  const topic = consistencyStore.topic(groupId, topicId);

  const breadcrumbsItems = [
    {
      title: t('consistency.breadcrumbs.home'),
      href: '/',
    },
    {
      title: t('consistency.breadcrumbs.title'),
      href: '/ui/consistency',
    },
    {
      title: groupId,
      href: `/ui/consistency/${groupId}`,
    },
    {
      title: topicId,
      href: `/ui/consistency/${groupId}/topics/${topicId}`,
    },
  ];

  async function doSyncTopic(datacenter: string) {
    const succeeded = await syncTopic(topicId, datacenter);
    if (succeeded) {
      router.push('/ui/consistency');
    }
  }

  async function doSyncSubscription(
    subscriptionQualifiedName: string,
    datacenter: string,
  ) {
    const subscriptionName = parseSubscriptionFqn(subscriptionQualifiedName);
    const succeeded = await syncSubscription(
      subscriptionName.topicName,
      subscriptionName.subscriptionName,
      datacenter,
    );
    if (succeeded) {
      router.push('/ui/consistency');
    }
  }
</script>

<template>
  <v-container>
    <v-row dense>
      <v-col md="12">
        <v-breadcrumbs :items="breadcrumbsItems" density="compact" />
      </v-col>
    </v-row>
    <v-row dense>
      <v-col md="12">
        <h3 class="text-h3">
          {{
            t('consistency.inconsistentGroup.inconsistentTopic.title', {
              topicId,
            })
          }}
        </h3>
      </v-col>
    </v-row>
    <InconsistentMetadata
      :metadata="topic.inconsistentMetadata"
      v-if="topic"
      class="mt-8"
      @sync="doSyncTopic"
    ></InconsistentMetadata>

    <v-card
      class="mt-8 mb-2"
      v-if="topic"
      :title="
        $t(
          'consistency.inconsistentGroup.inconsistentTopic.inconsistentSubscriptions',
        )
      "
    >
      <v-expansion-panels v-if="topic.inconsistentSubscriptions.length > 0">
        <v-expansion-panel
          v-for="subscription in topic.inconsistentSubscriptions"
          :key="subscription"
        >
          <v-expansion-panel-title>{{
            subscription.name
          }}</v-expansion-panel-title>
          <v-expansion-panel-text>
            <InconsistentMetadata
              :metadata="subscription.inconsistentMetadata"
              @sync="(dc) => doSyncSubscription(subscription.name, dc)"
            ></InconsistentMetadata>
          </v-expansion-panel-text>
        </v-expansion-panel>
      </v-expansion-panels>
      <v-banner
        v-else
        :text="
          $t('consistency.inconsistentGroup.inconsistentTopic.noSubscriptions')
        "
      ></v-banner>
    </v-card>
  </v-container>
</template>
