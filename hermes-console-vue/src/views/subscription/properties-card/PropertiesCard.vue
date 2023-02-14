<script setup lang="ts">
  import { dateFromTimestamp } from '@/utils/date-formatter/date-formatter';
  import { DeliveryType } from '@/api/subscription';
  import KeyValueCard from '@/components/key-value-card/KeyValueCard.vue';
  import type { Subscription } from '@/api/subscription';

  interface PropertiesCardProps {
    subscription: Subscription;
  }

  function getTrackingModeName(trackingMode: string): string {
    switch (trackingMode) {
      case 'trackingOff':
        return 'No tracking';
      case 'discardedOnly':
        return 'Track message discarding only';
      case 'trackingAll':
        return 'Track everything';
      default:
        return 'Unknown';
    }
  }

  const props = defineProps<PropertiesCardProps>();

  const entries = [
    {
      name: 'Content type',
      value: props.subscription.contentType,
    },
    {
      name: 'Delivery type',
      value: props.subscription.deliveryType,
      tooltip:
        'Hermes can deliver messages in SERIAL (one message at a time) or ' +
        'in BATCH (group of messages at a time).',
    },
    {
      name: 'Mode',
      value: props.subscription.mode,
      tooltip:
        'Hermes can deliver messages in ANYCAST (to one of subscribed ' +
        'hosts) or in BROADCAST (to all subscribed hosts) mode.',
    },
    {
      name: 'Rate limit',
      value: props.subscription.subscriptionPolicy.rate,
      displayIf: props.subscription.deliveryType === DeliveryType.SERIAL,
      tooltip:
        'Maximum rate defined by user (per data center). Maximum rate ' +
        'calculated by algorithm can be observed in "Output rate" metric.',
    },
    {
      name: 'Batch size',
      value: `${props.subscription.subscriptionPolicy.batchSize} messages`,
      displayIf: props.subscription.deliveryType === DeliveryType.BATCH,
      tooltip: 'Desired number of messages in a single batch.',
    },
    {
      name: 'Batch time window',
      value: `${props.subscription.subscriptionPolicy.batchTime} milliseconds`,
      displayIf: props.subscription.deliveryType === DeliveryType.BATCH,
      tooltip:
        'Max time between arrival of first message to batch delivery attempt.',
    },
    {
      name: 'Batch volume',
      value: `${props.subscription.subscriptionPolicy.batchVolume} bytes`,
      displayIf: props.subscription.deliveryType === DeliveryType.BATCH,
      tooltip: 'Desired number of bytes in single batch.',
    },
    {
      name: 'Request timeout',
      value: `${props.subscription.subscriptionPolicy.requestTimeout} milliseconds`,
      displayIf: props.subscription.deliveryType === DeliveryType.BATCH,
      tooltip: 'Desired number of bytes in single batch.',
    },
    {
      name: 'Sending delay',
      value: `${props.subscription.subscriptionPolicy.sendingDelay} milliseconds`,
      displayIf: props.subscription.deliveryType === DeliveryType.SERIAL,
      tooltip:
        'Amount of time in ms after which an event will be send. Useful if ' +
        'events from two topics are sent at the same time and you want to ' +
        'increase chance that events from one topic will be deliver after ' +
        'events from other topic.',
    },
    {
      name: 'Message TTL',
      value: `${props.subscription.subscriptionPolicy.messageTtl} seconds`,
      tooltip:
        'Amount of time a message can be held in sending queue and retried. ' +
        'If message will not be delivered during this time, it will be ' +
        'discarded.',
    },
    {
      name: 'Request timeout',
      value: `${props.subscription.subscriptionPolicy.requestTimeout} milliseconds`,
      tooltip: 'Http client request timeout in milliseconds.',
    },
    {
      name: 'Message delivery tracking',
      value: getTrackingModeName(props.subscription.trackingMode),
    },
    {
      name: 'Retry on 4xx status',
      value: props.subscription.subscriptionPolicy.retryClientErrors,
      tooltip:
        'If false, message will not be retried when service responds with ' +
        '4xx status (i.e. Bad Request).',
    },
    {
      name: 'Retry backoff',
      value: `${props.subscription.subscriptionPolicy.messageBackoff} milliseconds`,
      tooltip: 'Minimum amount of time between consecutive message retries.',
    },
    {
      name: 'Retry backoff multiplier',
      value: props.subscription.subscriptionPolicy.backoffMultiplier,
      displayIf: props.subscription.deliveryType === DeliveryType.SERIAL,
      tooltip:
        'Delay multiplier between consecutive send attempts of failed requests',
    },
    {
      name: 'Retry backoff max interval',
      value: props.subscription.subscriptionPolicy.backoffMaxIntervalInSec,
      displayIf:
        props.subscription.deliveryType === DeliveryType.SERIAL &&
        props.subscription.subscriptionPolicy.backoffMultiplier > 1,
      tooltip:
        'Maximum value of delay backoff when using exponential calculation',
    },
    {
      name: 'Monitoring severity',
      value: props.subscription.monitoringDetails.severity,
      tooltip:
        "How important should be the subscription's health for the monitoring.",
    },
    {
      name: 'Monitoring reaction',
      value: props.subscription.monitoringDetails.reaction,
      tooltip:
        'Information for monitoring how to react when the subscription ' +
        'becomes unhealthy (e.g. team name or Pager Duty ID).',
    },
    {
      name: 'Deliver using http/2',
      value: props.subscription.http2Enabled,
      tooltip: 'If true Hermes will deliver messages using http/2 protocol.',
    },
    {
      name: 'Attach subscription identity headers',
      value: props.subscription.subscriptionIdentityHeadersEnabled,
      tooltip:
        'If true Hermes will attach HTTP headers with subscription identity.',
    },
    {
      name: 'Automatically remove',
      value: props.subscription.autoDeleteWithTopicEnabled,
      tooltip:
        'When the associated topic is deleted, Hermes will delete the ' +
        'subscription automatically.',
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
      name: 'Creation date',
      value: dateFromTimestamp(props.subscription.createdAt),
    },
    {
      name: 'Modification date',
      value: dateFromTimestamp(props.subscription.modifiedAt),
    },
  ];
</script>

<template>
  <key-value-card :entries="entries" card-title="Properties" />
</template>

<style scoped lang="scss"></style>
