<script setup lang="ts">
  import { DeliveryType } from '@/api/subscription';
  import { formatTimestamp } from '@/utils/date-formatter/date-formatter';
  import { useI18n } from 'vue-i18n';
  import KeyValueCard from '@/components/key-value-card/KeyValueCard.vue';
  import type { Subscription } from '@/api/subscription';

  const props = defineProps<{
    subscription: Subscription;
  }>();

  const { t } = useI18n();

  function getTrackingModeName(trackingMode: string): string {
    switch (trackingMode) {
      case 'trackingOff':
        return t('subscription.propertiesCard.trackingOff');
      case 'discardedOnly':
        return t('subscription.propertiesCard.discardedOnly');
      case 'trackingAll':
        return t('subscription.propertiesCard.trackingAll');
      default:
        return t('subscription.propertiesCard.unknown');
    }
  }

  const entries = [
    {
      name: t('subscription.propertiesCard.contentType'),
      value: props.subscription.contentType,
    },
    {
      name: t('subscription.propertiesCard.deliveryType'),
      value: props.subscription.deliveryType,
      tooltip: t('subscription.propertiesCard.tooltips.deliveryType'),
    },
    {
      name: t('subscription.propertiesCard.mode'),
      value: props.subscription.mode,
      tooltip: t('subscription.propertiesCard.tooltips.mode'),
    },
    {
      name: t('subscription.propertiesCard.rateLimit'),
      value: props.subscription.subscriptionPolicy.rate,
      displayIf: props.subscription.deliveryType === DeliveryType.SERIAL,
      tooltip: t('subscription.propertiesCard.tooltips.rateLimit'),
    },
    {
      name: t('subscription.propertiesCard.batchSize'),
      value: props.subscription.subscriptionPolicy.batchSize,
      displayIf: props.subscription.deliveryType === DeliveryType.BATCH,
      tooltip: t('subscription.propertiesCard.tooltips.batchSize'),
    },
    {
      name: t('subscription.propertiesCard.batchTime'),
      value: `${props.subscription.subscriptionPolicy.batchTime} ms`,
      displayIf: props.subscription.deliveryType === DeliveryType.BATCH,
      tooltip: t('subscription.propertiesCard.tooltips.batchTime'),
    },
    {
      name: t('subscription.propertiesCard.batchVolume'),
      value: `${props.subscription.subscriptionPolicy.batchVolume} B`,
      displayIf: props.subscription.deliveryType === DeliveryType.BATCH,
      tooltip: t('subscription.propertiesCard.tooltips.batchVolume'),
    },
    {
      name: t('subscription.propertiesCard.requestTimeout'),
      value: `${props.subscription.subscriptionPolicy.requestTimeout} ms`,
      displayIf: props.subscription.deliveryType === DeliveryType.BATCH,
      tooltip: t('subscription.propertiesCard.tooltips.requestTimeout'),
    },
    {
      name: t('subscription.propertiesCard.sendingDelay'),
      value: `${props.subscription.subscriptionPolicy.sendingDelay} ms`,
      displayIf: props.subscription.deliveryType === DeliveryType.SERIAL,
      tooltip: t('subscription.propertiesCard.tooltips.sendingDelay'),
    },
    {
      name: t('subscription.propertiesCard.messageTtl'),
      value: `${props.subscription.subscriptionPolicy.messageTtl} s`,
      tooltip: t('subscription.propertiesCard.tooltips.messageTtl'),
    },
    {
      name: t('subscription.propertiesCard.requestTimeout'),
      value: `${props.subscription.subscriptionPolicy.requestTimeout} ms`,
      tooltip: t('subscription.propertiesCard.tooltips.requestTimeout'),
    },
    {
      name: t('subscription.propertiesCard.trackingMode'),
      value: getTrackingModeName(props.subscription.trackingMode),
    },
    {
      name: t('subscription.propertiesCard.retryClientErrors'),
      value: props.subscription.subscriptionPolicy.retryClientErrors,
      tooltip: t('subscription.propertiesCard.tooltips.retryClientErrors'),
    },
    {
      name: t('subscription.propertiesCard.retryBackoff'),
      value: `${props.subscription.subscriptionPolicy.messageBackoff} ms`,
      tooltip: t('subscription.propertiesCard.tooltips.retryBackoff'),
    },
    {
      name: t('subscription.propertiesCard.backoffMultiplier'),
      value: props.subscription.subscriptionPolicy.backoffMultiplier,
      displayIf: props.subscription.deliveryType === DeliveryType.SERIAL,
      tooltip: t('subscription.propertiesCard.tooltips.backoffMultiplier'),
    },
    {
      name: t('subscription.propertiesCard.backoffMaxInterval'),
      value: `${props.subscription.subscriptionPolicy.backoffMaxIntervalInSec} s`,
      displayIf:
        props.subscription.deliveryType === DeliveryType.SERIAL &&
        props.subscription.subscriptionPolicy.backoffMultiplier > 1,
      tooltip: t('subscription.propertiesCard.tooltips.backoffMaxInterval'),
    },
    {
      name: t('subscription.propertiesCard.monitoringSeverity'),
      value: props.subscription.monitoringDetails.severity,
      tooltip: t('subscription.propertiesCard.tooltips.monitoringSeverity'),
    },
    {
      name: t('subscription.propertiesCard.monitoringReaction'),
      value: props.subscription.monitoringDetails.reaction,
      tooltip:
        'Information for monitoring how to react when the subscription ' +
        'becomes unhealthy (e.g. team name or Pager Duty ID).',
    },
    {
      name: t('subscription.propertiesCard.http2'),
      value: props.subscription.http2Enabled,
      tooltip: t('subscription.propertiesCard.tooltips.http2'),
    },
    {
      name: t('subscription.propertiesCard.subscriptionIdentityHeaders'),
      value: props.subscription.subscriptionIdentityHeadersEnabled,
      tooltip: t(
        'subscription.propertiesCard.tooltips.subscriptionIdentityHeaders',
      ),
    },
    {
      name: t('subscription.propertiesCard.autoDeleteWithTopic'),
      value: props.subscription.autoDeleteWithTopicEnabled,
      tooltip: t('subscription.propertiesCard.tooltips.autoDeleteWithTopic'),
    },
    /*
     * TODO: subscription.html
     *
     * <p ng-repeat="(key, entry) in endpointAddressResolverMetadataConfig">
     *     <strong>{{entry.title}}:</strong>
     *     {{entry.options[subscription.endpointAddressResolverMetadata[key]] || subscription.endpointAddressResolverMetadata[key]}}
     *     <span ng-show="entry.hint" uib-popover="{{entry.hint}}" popover-trigger="mouseenter" class="fa helpme pull-right">&#xf128;</span>
     * </p>
     * <p ng-repeat="(key, value) in notSupportedEndpointAddressResolverMetadataEntries(subscription.endpointAddressResolverMetadata)">
     *     <strong>{{key}}:</strong> {{value}}
     * </p>
     */
    {
      name: t('subscription.propertiesCard.createdAt'),
      value: formatTimestamp(props.subscription.createdAt),
    },
    {
      name: t('subscription.propertiesCard.modifiedAt'),
      value: formatTimestamp(props.subscription.modifiedAt),
    },
  ];
</script>

<template>
  <key-value-card
    :entries="entries"
    :card-title="t('subscription.propertiesCard.title')"
  />
</template>

<style scoped lang="scss"></style>
