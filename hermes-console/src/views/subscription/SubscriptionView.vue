<script setup lang="ts">
  import { isSubscriptionOwnerOrAdmin } from '@/utils/roles-util';
  import { useAppConfigStore } from '@/store/app-config/useAppConfigStore';
  import { useDialog } from '@/composables/dialog/use-dialog/useDialog';
  import { useI18n } from 'vue-i18n';
  import { useRoles } from '@/composables/roles/use-roles/useRoles';
  import { useRouter } from 'vue-router';
  import { useSubscription } from '@/composables/subscription/use-subscription/useSubscription';
  import { useTopic } from '@/composables/topic/use-topic/useTopic';
  import ConfirmationDialog from '@/components/confirmation-dialog/ConfirmationDialog.vue';
  import ConsoleAlert from '@/components/console-alert/ConsoleAlert.vue';
  import CostsCard from '@/components/costs-card/CostsCard.vue';
  import FiltersCard from '@/views/subscription/filters-card/FiltersCard.vue';
  import HeadersCard from '@/views/subscription/headers-card/HeadersCard.vue';
  import HealthProblemsAlerts from '@/views/subscription/health-problems-alerts/HealthProblemsAlerts.vue';
  import LastUndeliveredMessage from '@/views/subscription/last-undelivered-message/LastUndeliveredMessage.vue';
  import LoadingSpinner from '@/components/loading-spinner/LoadingSpinner.vue';
  import ManageMessagesCard from '@/views/subscription/manage-messages-card/ManageMessagesCard.vue';
  import MetricsCard from '@/views/subscription/metrics-card/MetricsCard.vue';
  import PropertiesCard from '@/views/subscription/properties-card/PropertiesCard.vue';
  import SubscriptionMetadata from '@/views/subscription/subscription-metadata/SubscriptionMetadata.vue';
  import TrackingCard from '@/components/tracking-card/TrackingCard.vue';
  import UndeliveredMessagesCard from '@/views/subscription/undelivered-messages-card/UndeliveredMessagesCard.vue';

  const router = useRouter();
  const { groupId, subscriptionId, topicId } = router.currentRoute.value
    .params as Record<string, string>;
  const { topic } = useTopic(topicId);
  const { t } = useI18n();

  const {
    subscription,
    owner,
    subscriptionMetrics,
    subscriptionHealth,
    subscriptionUndeliveredMessages,
    subscriptionLastUndeliveredMessage,
    trackingUrls,
    retransmitting,
    skippingAllMessages,
    error,
    loading,
    removeSubscription,
    suspendSubscription,
    activateSubscription,
    retransmitMessages,
    skipAllMessages,
  } = useSubscription(topicId, subscriptionId);

  const roles = useRoles(topicId, subscriptionId)?.roles;

  const {
    isDialogOpened: isRemoveDialogOpened,
    actionButtonEnabled: removeActionButtonEnabled,
    openDialog: openRemoveDialog,
    closeDialog: closeRemoveDialog,
    enableActionButton: enableRemoveActionButton,
    disableActionButton: disableRemoveActionButton,
  } = useDialog();

  async function deleteSubscription() {
    disableRemoveActionButton();
    const isSubscriptionRemoved = await removeSubscription();
    enableRemoveActionButton();
    closeRemoveDialog();
    if (isSubscriptionRemoved) {
      router.push({ path: `/ui/groups/${groupId}/topics/${topicId}` });
    }
  }

  const {
    isDialogOpened: isSuspendDialogOpened,
    actionButtonEnabled: actionSuspendButtonEnabled,
    openDialog: openSuspendDialog,
    closeDialog: closeSuspendDialog,
    enableActionButton: enableSuspendActionButton,
    disableActionButton: disableSuspendActionButton,
  } = useDialog();

  async function suspend() {
    disableSuspendActionButton();
    const isSubscriptionSuspended = await suspendSubscription();
    enableSuspendActionButton();
    closeSuspendDialog();
    if (isSubscriptionSuspended) {
      router.go(0);
    }
  }

  const {
    isDialogOpened: isActivateDialogOpened,
    actionButtonEnabled: actionActivateButtonEnabled,
    openDialog: openActivateDialog,
    closeDialog: closeActivateDialog,
    enableActionButton: enableActivateActionButton,
    disableActionButton: disableActivateActionButton,
  } = useDialog();

  async function activate() {
    disableActivateActionButton();
    const isSubscriptionActivated = await activateSubscription();
    enableActivateActionButton();
    closeActivateDialog();
    if (isSubscriptionActivated) {
      router.go(0);
    }
  }

  const onRetransmit = async (fromDate: string) => {
    await retransmitMessages(fromDate);
  };

  const onSkipAllMessages = async () => {
    await skipAllMessages();
  };

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
      title: topicId,
      href: `/ui/groups/${groupId}/topics/${topicId}`,
    },
    {
      title: subscriptionId,
    },
  ];

  const configStore = useAppConfigStore();

  function resolveCostsUrl(url?: string): string {
    return (
      url
        ?.replace('{{topic_name}}', topicId)
        .replace('{{subscription_name}}', subscriptionId) ?? ''
    );
  }

  const costs = {
    iframeUrl: resolveCostsUrl(
      configStore.appConfig?.costs.subscriptionIframeUrl,
    ),
    detailsUrl: resolveCostsUrl(
      configStore.appConfig?.costs.subscriptionDetailsUrl,
    ),
  };
</script>

<template>
  <confirmation-dialog
    v-model="isRemoveDialogOpened"
    :actionButtonEnabled="removeActionButtonEnabled"
    :title="$t('subscription.confirmationDialog.remove.title')"
    :text="t('subscription.confirmationDialog.remove.text', { subscriptionId })"
    @action="deleteSubscription"
    @cancel="closeRemoveDialog"
  />
  <confirmation-dialog
    v-model="isSuspendDialogOpened"
    :actionButtonEnabled="actionSuspendButtonEnabled"
    :title="$t('subscription.confirmationDialog.suspend.title')"
    :text="
      t('subscription.confirmationDialog.suspend.text', { subscriptionId })
    "
    @action="suspend"
    @cancel="closeSuspendDialog"
  />
  <confirmation-dialog
    v-model="isActivateDialogOpened"
    :actionButtonEnabled="actionActivateButtonEnabled"
    :title="$t('subscription.confirmationDialog.activate.title')"
    :text="
      t('subscription.confirmationDialog.activate.text', { subscriptionId })
    "
    @action="activate"
    @cancel="closeActivateDialog"
  />
  <v-container>
    <v-row dense>
      <v-col md="12">
        <v-breadcrumbs :items="breadcrumbsItems" density="compact" />
        <loading-spinner v-if="loading" />
        <console-alert
          v-if="error.fetchSubscription"
          :title="$t('subscription.connectionError.title')"
          :text="t('subscription.connectionError.text', { subscriptionId })"
          type="error"
        />
      </v-col>
    </v-row>

    <template v-if="!loading && !error.fetchSubscription">
      <v-row dense>
        <v-col md="12">
          <health-problems-alerts
            v-if="subscriptionHealth && subscriptionHealth.problems"
            :problems="subscriptionHealth?.problems"
          />
          <subscription-metadata
            v-if="subscription && owner"
            :subscription="subscription"
            :owner="owner"
            :roles="roles"
            :schema="topic?.schema"
            @remove="openRemoveDialog"
            @suspend="openSuspendDialog"
            @activate="openActivateDialog"
          />
        </v-col>
      </v-row>

      <v-row dense>
        <v-col md="6" class="d-flex flex-column row-gap-2">
          <metrics-card
            v-if="subscriptionMetrics"
            :subscription-metrics="subscriptionMetrics"
            :topic-name="topicId"
            :subscription-name="subscriptionId"
          />
          <costs-card
            v-if="configStore.appConfig?.costs.enabled"
            :iframe-url="costs.iframeUrl"
            :details-url="costs.detailsUrl"
          />
          <tracking-card
            v-if="subscription?.trackingEnabled"
            :tracking-urls="trackingUrls"
          />
          <manage-messages-card
            v-if="isSubscriptionOwnerOrAdmin(roles)"
            :topic="topicId"
            :subscription="subscriptionId"
            :retransmitting="retransmitting"
            :skippingAllMessages="skippingAllMessages"
            @retransmit="onRetransmit"
            @skipAllMessages="onSkipAllMessages"
          />
          <last-undelivered-message
            v-if="
              subscriptionLastUndeliveredMessage &&
              isSubscriptionOwnerOrAdmin(roles)
            "
            :last-undelivered="subscriptionLastUndeliveredMessage"
          />
        </v-col>
        <v-col md="6">
          <properties-card v-if="subscription" :subscription="subscription" />
        </v-col>
      </v-row>

      <v-row dense>
        <v-col md="12">
          <filters-card
            v-if="subscription && subscription?.filters.length > 0"
            :filters="subscription?.filters!!"
            :schema="topic?.schema"
            :topic="topicId"
          />
          <headers-card
            v-if="!!subscription && subscription.headers.length > 0"
            :headers="subscription?.headers"
          />
          <undelivered-messages-card
            v-if="
              subscriptionUndeliveredMessages &&
              subscriptionUndeliveredMessages?.length > 0 &&
              isSubscriptionOwnerOrAdmin(roles)
            "
            :undelivered-messages="subscriptionUndeliveredMessages"
          />
        </v-col>
      </v-row>
    </template>
  </v-container>
</template>

<style scoped lang="scss"></style>
