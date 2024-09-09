import { computed, ref } from 'vue';
import { dummyAppConfig } from '@/dummy/app-config';
import { dummyOwner } from '@/dummy/topic';
import { dummySubscription } from '@/dummy/subscription';
import { matchRegex, max, min, required } from '@/utils/validators';
import type { DataSources } from '@/composables/subscription/use-form-subscription/types';

export const dummySubscriptionForm = {
  name: '',
  endpoint: '',
  description: '',
  ownerSource: null,
  owner: '',
  ownerSearch: '',
  contentType: '',
  deliveryType: '',
  mode: '',
  subscriptionPolicy: {
    rateLimit: null,
    inflightMessageTTL: 3600,
    inflightMessagesCount: null,
    retryBackoff: 1000,
    retryBackoffMultiplier: 1.0,
    backoffMaxIntervalInSec: 600,
    sendingDelay: 0,
    requestTimeout: 1000,
    batchSize: null,
    batchTime: null,
    batchVolume: null,
  },
  retryOn4xx: false,
  messageDeliveryTrackingMode: '',
  monitoringDetails: {
    severity: '',
    reaction: '',
  },
  deliverUsingHttp2: false,
  attachSubscriptionIdentityHeaders: false,
  deleteSubscriptionAutomatically: false,
  pathFilters: [],
  headerFilters: [],
  endpointAddressResolverMetadata: {
    supportedMetadata: false,
  },
};

export const dummySubscriptionFormValidator = {
  name: [required(), matchRegex(/^[a-zA-Z0-9.-]+$/, 'Invalid name')],
  endpoint: [required()],
  description: [required()],
  ownerSource: [required()],
  owner: [required()],
  contentType: [required()],
  deliveryType: [required()],
  mode: [required()],
  rateLimit: [required(), min(0), max(5000)],
  batchSize: [required(), min(1), max(1000000)],
  batchTime: [required(), min(1), max(1000000)],
  batchVolume: [required(), min(1), max(1000000)],
  requestTimeout: [required(), min(0), max(10000)],
  sendingDelay: [required(), min(0), max(5000)],
  inflightMessageTTL: [required(), min(0), max(7200)],
  inflightMessagesCount: [min(1)],
  retryBackoff: [required(), min(0), max(1000000)],
  retryBackoffMultiplier: [required(), min(1), max(10)],
  backoffMaxIntervalInSec: [required(), min(1), max(600)],
  messageDeliveryTrackingMode: [required()],
  monitoringSeverity: [required()],
};

export const dummyContentTypes = [
  { title: 'JSON', value: 'JSON', unsupportedDeliveryTypes: [] },
  {
    title: 'AVRO (not supported in BATCH delivery mode)',
    value: 'AVRO',
    unsupportedDeliveryTypes: ['BATCH'],
  },
];

export const dummyDeliveryTypes = dummyAppConfig.subscription.deliveryTypes.map(
  (type) => {
    return {
      title: type.label,
      value: type.value,
    };
  },
);

export const dummyOwnerSources = [
  {
    name: 'Service Catalog',
    autocomplete: true,
    deprecated: false,
  },
];

export const dummyDeliveryModes = [
  { title: 'ANYCAST', value: 'ANYCAST' },
  { title: 'BROADCAST (incubating feature)', value: 'BROADCAST' },
];
export const dummyMonitoringSeverities = [
  { title: 'Non-important', value: 'NON_IMPORTANT' },
  { title: 'Important', value: 'IMPORTANT' },
  { title: 'Critical', value: 'CRITICAL' },
];
export const dummyMessageDeliveryTrackingModes = [
  { title: 'No tracking', value: 'trackingOff' },
  { title: 'Track message discarding only', value: 'discardedOnly' },
  { title: 'Track everything', value: 'trackingAll' },
];

export const dummyDataSources: DataSources = {
  contentTypes: computed(() => dummyContentTypes),
  deliveryModes: dummyDeliveryModes,
  deliveryTypes: dummyContentTypes,
  monitoringSeverities: dummyMonitoringSeverities,
  messageDeliveryTrackingModes: dummyMessageDeliveryTrackingModes,
  ownerSources: computed(() =>
    dummyOwnerSources
      .filter((source) => !source.deprecated)
      .map((source) => {
        return { title: source.name, value: source };
      }),
  ),
  owners: ref(
    [dummyOwner].map((source) => {
      return { title: source.name, value: source.id };
    }),
  ),
  loadingOwners: ref(false),
};

export const dummyInitializedSubscriptionForm = {
  name: '',
  endpoint: '',
  description: '',
  ownerSource: null,
  owner: '',
  ownerSearch: '',
  contentType: '',
  deliveryType: dummyAppConfig.subscription.defaults.deliveryType,
  mode: dummyDeliveryModes[0].value,
  subscriptionPolicy: {
    rateLimit: null,
    inflightMessageTTL:
      dummyAppConfig.subscription.defaults.subscriptionPolicy.messageTtl ||
      3600,
    inflightMessagesCount: null,
    retryBackoff: 1000,
    retryBackoffMultiplier: 1.0,
    backoffMaxIntervalInSec: 600,
    sendingDelay: 0,
    requestTimeout:
      dummyAppConfig.subscription.defaults.subscriptionPolicy.requestTimeout ||
      1000,
    batchSize: null,
    batchTime: null,
    batchVolume: null,
  },
  retryOn4xx: false,
  messageDeliveryTrackingMode: dummyMessageDeliveryTrackingModes[0].value,
  monitoringDetails: {
    severity: dummyMonitoringSeverities[0].value,
    reaction: '',
  },
  deliverUsingHttp2: false,
  attachSubscriptionIdentityHeaders: false,
  deleteSubscriptionAutomatically: false,
  pathFilters: [],
  headerFilters: [],
  endpointAddressResolverMetadata: {
    supportedMetadata: false,
  },
};

export const dummyInitializedEditSubscriptionForm = {
  name: dummySubscription.name,
  endpoint: dummySubscription.endpoint,
  description: dummySubscription.description,
  ownerSource: null,
  owner: dummySubscription.owner.id,
  ownerSearch: '',
  contentType: dummySubscription.contentType,
  deliveryType: dummyAppConfig.subscription.defaults.deliveryType,
  mode: dummySubscription.mode,
  subscriptionPolicy: {
    rateLimit: dummySubscription.subscriptionPolicy.rate,
    inflightMessageTTL: dummySubscription.subscriptionPolicy.messageTtl,
    inflightMessagesCount: dummySubscription.subscriptionPolicy.inflightSize,
    retryBackoff: dummySubscription.subscriptionPolicy.messageBackoff,
    retryBackoffMultiplier:
      dummySubscription.subscriptionPolicy.backoffMultiplier,
    backoffMaxIntervalInSec:
      dummySubscription.subscriptionPolicy.backoffMaxIntervalInSec,
    sendingDelay: dummySubscription.subscriptionPolicy.sendingDelay,
    requestTimeout: dummySubscription.subscriptionPolicy.requestTimeout,
    batchSize: null,
    batchTime: null,
    batchVolume: null,
  },
  retryOn4xx: dummySubscription.subscriptionPolicy.retryClientErrors,
  messageDeliveryTrackingMode: dummySubscription.trackingMode,
  monitoringDetails: dummySubscription.monitoringDetails,
  deliverUsingHttp2: dummySubscription.http2Enabled,
  attachSubscriptionIdentityHeaders:
    dummySubscription.subscriptionIdentityHeadersEnabled,
  deleteSubscriptionAutomatically: dummySubscription.autoDeleteWithTopicEnabled,
  pathFilters: [],
  headerFilters: [],
  endpointAddressResolverMetadata: {
    supportedMetadata: true,
  },
};
