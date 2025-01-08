<script async setup lang="ts">
  import { copyToClipboard } from '@/utils/copy-utils';
  import { isTopicOwnerOrAdmin } from '@/utils/roles-util';
  import { useAppConfigStore } from '@/store/app-config/useAppConfigStore';
  import { useDialog } from '@/composables/dialog/use-dialog/useDialog';
  import { useI18n } from 'vue-i18n';
  import { useRoles } from '@/composables/roles/use-roles/useRoles';
  import { useRouter } from 'vue-router';
  import { useTopic } from '@/composables/topic/use-topic/useTopic';
  import ConfirmationDialog from '@/components/confirmation-dialog/ConfirmationDialog.vue';
  import ConsoleAlert from '@/components/console-alert/ConsoleAlert.vue';
  import CostsCard from '@/components/costs-card/CostsCard.vue';
  import LoadingSpinner from '@/components/loading-spinner/LoadingSpinner.vue';
  import MessagesPreview from '@/views/topic/messages-preview/MessagesPreview.vue';
  import MetricsList from '@/views/topic/metrics-list/MetricsList.vue';
  import OfflineClients from '@/views/topic/offline-clients/OfflineClients.vue';
  import PropertiesList from '@/views/topic/properties-list/PropertiesList.vue';
  import SchemaPanel from '@/views/topic/schema-panel/SchemaPanel.vue';
  import SubscriptionsList from '@/views/topic/subscriptions-list/SubscriptionsList.vue';
  import TopicHeader from '@/views/topic/topic-header/TopicHeader.vue';
  import TrackingCard from '@/components/tracking-card/TrackingCard.vue';

  const router = useRouter();

  const { t } = useI18n();

  const { groupId, topicName } = router.currentRoute.value.params as Record<
    string,
    string
  >;

  const {
    topic,
    owner,
    messages,
    metrics,
    loading,
    error,
    subscriptions,
    offlineClientsSource,
    trackingUrls,
    fetchOfflineClientsSource,
    removeTopic,
    fetchTopicClients,
  } = useTopic(topicName);

  const breadcrumbsItems = [
    {
      title: t('subscription.subscriptionBreadcrumbs.home'),
      href: '/',
    },
    {
      title: t('subscription.subscriptionBreadcrumbs.groups'),
      href: '/ui/groups',
    },
    {
      title: groupId,
      href: `/ui/groups/${groupId}`,
    },
    {
      title: topicName,
      href: `/ui/groups/${groupId}/topics/${topicName}`,
    },
  ];
  const configStore = useAppConfigStore();
  if (configStore.appConfig?.topic.offlineClientsEnabled) {
    fetchOfflineClientsSource();
  }
  const roles = useRoles(topicName, null)?.roles;

  const {
    isDialogOpened: isRemoveDialogOpened,
    actionButtonEnabled: removeActionButtonEnabled,
    openDialog: openRemoveDialog,
    closeDialog: closeRemoveDialog,
    enableActionButton: enableRemoveActionButton,
    disableActionButton: disableRemoveActionButton,
  } = useDialog();

  async function deleteTopic() {
    disableRemoveActionButton();
    const isTopicRemoved = await removeTopic();
    enableRemoveActionButton();
    closeRemoveDialog();
    if (isTopicRemoved) {
      router.push({ path: `/ui/groups/${groupId}` });
    }
  }

  async function copyClientsToClipboard() {
    const clients = await fetchTopicClients();

    if (clients != null) {
      copyToClipboard(clients.join(','));
    }
  }

  function resolveCostsUrl(url?: string): string {
    return url?.replace('{{topic_name}}', topicName) ?? '';
  }

  const costs = {
    iframeUrl: resolveCostsUrl(configStore.appConfig?.costs.topicIframeUrl),
    detailsUrl: resolveCostsUrl(configStore.appConfig?.costs.topicDetailsUrl),
  };
</script>

<template>
  <confirmation-dialog
    v-model="isRemoveDialogOpened"
    :actionButtonEnabled="removeActionButtonEnabled"
    :title="$t('topicView.confirmationDialog.remove.title')"
    :text="t('topicView.confirmationDialog.remove.text', { topicName })"
    @action="deleteTopic"
    @cancel="closeRemoveDialog"
  />
  <v-container class="d-flex flex-column topic-view__container">
    <div class="d-flex justify-space-between align-center">
      <v-breadcrumbs :items="breadcrumbsItems" density="compact" />
    </div>
    <loading-spinner v-if="loading" />
    <console-alert
      v-if="error.fetchTopic"
      :text="
        t('topicView.errorMessage.topicFetchFailed', { topicName: topicName })
      "
      type="error"
    />

    <template v-if="!loading && !error.fetchTopic">
      <topic-header
        v-if="topic && owner"
        :topic="topic"
        :owner="owner"
        :roles="roles"
        @remove="openRemoveDialog"
      />

      <v-row dense>
        <v-col md="6" class="d-flex flex-column row-gap-2">
          <metrics-list
            v-if="metrics"
            :metrics="metrics"
            :topic-name="topicName"
          />
          <costs-card
            v-if="configStore.appConfig?.costs.enabled"
            :iframe-url="costs.iframeUrl"
            :details-url="costs.detailsUrl"
          />
          <tracking-card
            v-if="topic?.trackingEnabled"
            :tracking-urls="trackingUrls"
          />
        </v-col>
        <v-col md="6">
          <properties-list v-if="topic" :topic="topic" />
        </v-col>
      </v-row>

      <schema-panel v-if="topic" :schema="topic.schema" />

      <messages-preview
        v-if="
          messages &&
          configStore.appConfig?.topic.messagePreviewEnabled &&
          isTopicOwnerOrAdmin(roles)
        "
        :messages="messages"
      />

      <subscriptions-list
        :groupId="groupId"
        :topic-name="topicName"
        :subscriptions="subscriptions ? subscriptions : []"
        :roles="roles"
        @copyClientsClick="copyClientsToClipboard"
      />

      <offline-clients
        v-if="
          configStore.appConfig?.topic.offlineClientsEnabled &&
          offlineClientsSource?.source &&
          topic?.offlineStorage.enabled
        "
        :source="offlineClientsSource.source"
      />
    </template>
  </v-container>
</template>

<style scoped lang="scss">
  .topic-view__container {
    row-gap: 8pt;
  }

  .topic-view__upper_panel {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
    grid-gap: 8pt;
    align-items: start;
  }
</style>
