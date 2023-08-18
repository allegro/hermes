import type {
  BatchSubscriptionPolicyJson,
  CreateSubscriptionFormRequestBody,
  SerialSubscriptionPolicyJson,
  SubscriptionFilterJson,
  SubscriptionPolicyJson,
} from '@/api/subscription';
import type { HeaderFilter } from '@/views/subscription/subscription-form/subscription-header-filters/types';
import type { PathFilter } from '@/views/subscription/subscription-form/subscription-basic-filters/types';
import type { SubscriptionForm } from '@/composables/subscription/use-create-subscription/types';

export function parseFormToRequestBody(
  topic: string,
  form: SubscriptionForm,
): CreateSubscriptionFormRequestBody {
  const pathFilters = form.pathFilters.map((filter) =>
    mapPathFilter(form.contentType, filter),
  );
  const headerFilters = form.headerFilters.map((filter) =>
    mapHeaderFilter(filter),
  );
  return {
    name: form.name,
    topicName: topic,
    owner: {
      source: form.ownerSource!!.name,
      id: form.owner,
    },
    contentType: form.contentType,
    deliveryType: form.deliveryType,
    description: form.description,
    endpoint: form.endpoint,
    filters: pathFilters.concat(headerFilters),
    headers: [],
    http2Enabled: form.deliverUsingHttp2,
    mode: form.mode,
    monitoringDetails: {
      reaction: form.monitoringDetails.reaction,
      severity: form.monitoringDetails.severity,
    },
    subscriptionPolicy: mapSubscriptionPolicy(form),
    trackingMode: form.messageDeliveryTrackingMode,
    endpointAddressResolverMetadata: {},
    subscriptionIdentityHeadersEnabled: form.attachSubscriptionIdentityHeaders,
    autoDeleteWithTopicEnabled: form.deleteSubscriptionAutomatically,
  };
}

function mapSubscriptionPolicy(form: SubscriptionForm): SubscriptionPolicyJson {
  return form.deliveryType === 'SERIAL'
    ? mapSerialSubscriptionPolicy(form)
    : mapBatchSerialSubscriptionPolicy(form);
}

function mapSerialSubscriptionPolicy(
  form: SubscriptionForm,
): SerialSubscriptionPolicyJson {
  return {
    backoffMaxIntervalInSec: 600,
    backoffMultiplier: form.subscriptionPolicy.retryBackoffMultiplier,
    messageBackoff: form.subscriptionPolicy.retryBackoff,
    messageTtl: form.subscriptionPolicy.inflightMessageTTL,
    rate: form.subscriptionPolicy.rateLimit!!,
    requestTimeout: form.subscriptionPolicy.requestTimeout,
    sendingDelay: form.subscriptionPolicy.sendingDelay,
    retryClientErrors: form.retryOn4xx,
  };
}

function mapBatchSerialSubscriptionPolicy(
  form: SubscriptionForm,
): BatchSubscriptionPolicyJson {
  return {
    messageTtl: form.subscriptionPolicy.inflightMessageTTL,
    retryClientsErrors: form.retryOn4xx,
    messageBackoff: form.subscriptionPolicy.retryBackoff,
    requestTimeout: form.subscriptionPolicy.requestTimeout,
    batchSize: form.subscriptionPolicy.batchSize!!,
    batchTime: form.subscriptionPolicy.batchTime!!,
    batchVolume: form.subscriptionPolicy.batchVolume!!,
  };
}

function mapPathFilter(
  contentType: string,
  filter: PathFilter,
): SubscriptionFilterJson {
  return {
    type: contentType === 'JSON' ? 'jsonpath' : 'avropath',
    path: filter.path,
    matcher: filter.matcher,
    matchingStrategy: filter.matchingStrategy,
  };
}

function mapHeaderFilter(filter: HeaderFilter): SubscriptionFilterJson {
  return {
    type: 'header',
    header: filter.headerName,
    matcher: filter.matcher,
  };
}
