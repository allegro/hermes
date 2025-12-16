<script async setup lang="ts">
  import { computed, ref, watch } from 'vue';
  import { copyToClipboard } from '@/utils/copy-utils';
  import { isTopicOwnerOrAdmin } from '@/utils/roles-util';
  import { useAppConfigStore } from '@/store/app-config/useAppConfigStore';
  import { useDialog } from '@/composables/dialog/use-dialog/useDialog';
  import { useI18n } from 'vue-i18n';
  import { useRoles } from '@/composables/roles/use-roles/useRoles';
  import { useRoute, useRouter } from 'vue-router';
  import { useTopic } from '@/composables/topic/use-topic/useTopic';
  import ConfirmationDialog from '@/components/confirmation-dialog/ConfirmationDialog.vue';
  import ConsoleAlert from '@/components/console-alert/ConsoleAlert.vue';
  import CostsCard from '@/components/costs-card/CostsCard.vue';
  import LoadingSpinner from '@/components/loading-spinner/LoadingSpinner.vue';
  import MessagesPreview from '@/views/topic/messages-preview/MessagesPreview.vue';
  import MetricsList from '@/views/topic/metrics-list/MetricsList.vue';
  import OfflineClients from '@/views/topic/offline-clients/OfflineClients.vue';
  import OfflineRetransmissionInfo from '@/views/topic/offline-retransmission/OfflineRetransmissionInfo.vue';
  import PropertiesList from '@/views/topic/properties-list/PropertiesList.vue';
  import SchemaPanel from '@/views/topic/schema-panel/SchemaPanel.vue';
  import SubscriptionsList from '@/views/topic/subscriptions-list/SubscriptionsList.vue';
  import TopicHeader from '@/views/topic/topic-header/TopicHeader.vue';
  import TrackingCard from '@/components/tracking-card/TrackingCard.vue';

  const router = useRouter();
  const route = useRoute();

  const { t } = useI18n();

  const groupId = computed(() => route.params.groupId as string);
  const topicName = computed(() => route.params.topicName as string);

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
    activeRetransmissions,
  } = useTopic(topicName);

  const breadcrumbsItems = computed(() => [
    {
      title: t('subscription.subscriptionBreadcrumbs.home'),
      href: '/',
    },
    {
      title: t('subscription.subscriptionBreadcrumbs.groups'),
      href: '/ui/groups',
    },
    {
      title: groupId.value,
      href: `/ui/groups/${groupId.value}`,
    },
    {
      title: topicName.value,
      href: `/ui/groups/${groupId.value}/topics/${topicName.value}`,
    },
  ]);
  const configStore = useAppConfigStore();
  watch(
    () => [
      configStore.appConfig?.topic?.offlineClientsEnabled,
      groupId,
      topicName,
    ],
    () => {
      if (configStore.appConfig?.topic.offlineClientsEnabled) {
        fetchOfflineClientsSource();
      }
    },
    { immediate: true },
  );

  const { roles } = useRoles(topicName, null);

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
      router.push({ path: `/ui/groups/${groupId.value}` });
    }
  }

  async function copyClientsToClipboard() {
    const clients = await fetchTopicClients();

    if (clients != null) {
      copyToClipboard(clients.join(','));
    }
  }

  function resolveCostsUrl(url?: string): string {
    return url?.replace('{{topic_name}}', topicName.value) ?? '';
  }

  const costs = computed(() => ({
    iframeUrl: resolveCostsUrl(configStore.appConfig?.costs.topicIframeUrl),
    detailsUrl: resolveCostsUrl(configStore.appConfig?.costs.topicDetailsUrl),
  }));

  const showOfflineClientsTab = computed(
    () =>
      configStore.appConfig?.topic.offlineClientsEnabled &&
      offlineClientsSource.value?.source &&
      topic.value?.offlineStorage.enabled,
  );

  const Tab = {
    General: 'general',
    Schema: 'schema',
    Subscriptions: 'subscriptions',
    OfflineClients: 'offlineClients',
    Messages: 'messages',
    OfflineRetransmission: 'offlineRetransmission',
  };
  const currentTab = ref<string>(Tab.General);
</script>

<template>
  <confirmation-dialog
    v-model="isRemoveDialogOpened"
    :actionButtonEnabled="removeActionButtonEnabled"
    :title="$t('topicView.confirmationDialog.remove.title')"
    :text="$t('topicView.confirmationDialog.remove.text', { topicName })"
    @action="deleteTopic"
    @cancel="closeRemoveDialog"
  />

  <v-container class="d-flex flex-column row-gap-2">
    <div class="d-flex justify-space-between align-center">
      <v-breadcrumbs :items="breadcrumbsItems" class="text-body-2" />
    </div>
    <loading-spinner v-if="loading" />
    <console-alert
      v-if="error.fetchTopic"
      :text="
        $t('topicView.errorMessage.topicFetchFailed', { topicName: topicName })
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

      <v-container class="py-0">
        <v-tabs v-model="currentTab" color="primary" class="topic-view__tabs">
          <v-tab :value="Tab.General" class="text-capitalize"
            >{{ $t('topicView.tabs.general') }}
          </v-tab>
          <v-tab :value="Tab.Schema" class="text-capitalize"
            >{{ $t('topicView.tabs.schema') }}
          </v-tab>
          <v-tab :value="Tab.Subscriptions" class="text-capitalize">
            {{ $t('topicView.tabs.subscriptions') }}
          </v-tab>
          <v-tab
            v-if="showOfflineClientsTab"
            :value="Tab.OfflineClients"
            class="text-capitalize"
          >
            {{ $t('topicView.tabs.offlineClients') }}
          </v-tab>
          <v-tab :value="Tab.Messages" class="text-capitalize">
            {{ $t('topicView.tabs.messages') }}
          </v-tab>
          <v-tab :value="Tab.OfflineRetransmission" class="text-capitalize">
            {{ $t('topicView.tabs.offlineRetransmission') }}
          </v-tab>
        </v-tabs>
      </v-container>

      <v-tabs-window v-model="currentTab">
        <v-tabs-window-item :value="Tab.General">
          <v-container class="py-0">
            <v-row>
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
              </v-col>
              <v-col md="6">
                <properties-list v-if="topic" :topic="topic" />
              </v-col>
            </v-row>
          </v-container>
        </v-tabs-window-item>

        <v-tabs-window-item :value="Tab.Schema">
          <v-container class="py-0">
            <schema-panel v-if="topic" :schema="topic.schema" />
          </v-container>
        </v-tabs-window-item>

        <v-tabs-window-item :value="Tab.Subscriptions">
          <v-container class="py-0">
            <subscriptions-list
              :groupId="groupId"
              :topic-name="topicName"
              :subscriptions="subscriptions || []"
              :roles="roles"
              @copyClientsClick="copyClientsToClipboard"
            />
          </v-container>
        </v-tabs-window-item>

        <v-tabs-window-item
          v-if="showOfflineClientsTab"
          :value="Tab.OfflineClients"
        >
          <v-container class="py-0">
            <offline-clients :source="offlineClientsSource.source" />
          </v-container>
        </v-tabs-window-item>

        <v-tabs-window-item :value="Tab.Messages">
          <v-container class="py-0">
            <v-row>
              <v-col md="12">
                <tracking-card
                  v-if="topic?.trackingEnabled"
                  :tracking-urls="trackingUrls"
                />
              </v-col>
            </v-row>
            <v-row>
              <v-col md="12">
                <messages-preview
                  :enabled="
                    configStore.appConfig?.topic.messagePreviewEnabled &&
                    isTopicOwnerOrAdmin(roles)
                  "
                  :messages="messages || []"
                />
              </v-col>
            </v-row>
          </v-container>
        </v-tabs-window-item>

        <v-tabs-window-item :value="Tab.OfflineRetransmission">
          <v-container class="py-0">
            <offline-retransmission-info
              :topic="topic"
              :roles="roles"
              :tasks="activeRetransmissions"
            />
          </v-container>
        </v-tabs-window-item>
      </v-tabs-window>
    </template>
  </v-container>
</template>

<style scoped lang="scss">
  .topic-view__container {
    row-gap: 8pt;
  }

  .topic-view__tabs {
    :deep(.v-slide-group__content) {
      border-bottom: 1px rgba(var(--v-border-color), var(--v-border-opacity))
        solid;
    }
  }
</style>
