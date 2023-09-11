import { dummyAppConfig } from '@/dummy/app-config';
import { dummySubscription } from '@/dummy/subscription';
import { matchRegex, max, min, required } from '@/utils/validators';

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
    retryBackoff: 1000,
    retryBackoffMultiplier: 1.0,
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
  retryBackoff: [required(), min(0), max(1000000)],
  retryBackoffMultiplier: [required(), min(1), max(10)],
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
    retryBackoff: 1000,
    retryBackoffMultiplier: 1.0,
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
    retryBackoff: dummySubscription.subscriptionPolicy.messageBackoff,
    retryBackoffMultiplier:
      dummySubscription.subscriptionPolicy.backoffMultiplier,
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
};
