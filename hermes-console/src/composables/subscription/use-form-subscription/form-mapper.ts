import type {
  BatchSubscriptionPolicyJson,
  CreateSubscriptionFormRequestBody,
  SerialSubscriptionPolicyJson,
  SubscriptionFilterJson,
  SubscriptionPolicyJson,
} from '@/api/subscription';
import type { HeaderFilter } from '@/views/subscription/subscription-form/subscription-header-filters/types';
import type { PathFilter } from '@/views/subscription/subscription-form/subscription-basic-filters/types';
import type { SubscriptionForm } from '@/composables/subscription/use-form-subscription/types';

export function parseFormToRequestBody(
  topic: string,
  form: SubscriptionForm,
  topicContentType: string,
): CreateSubscriptionFormRequestBody {
  const pathFilters = form.pathFilters.map((filter: PathFilter) =>
    mapPathFilter(topicContentType, filter),
  );
  const headerFilters = form.headerFilters.map((filter: HeaderFilter) =>
    mapHeaderFilter(filter),
  );
  const parsedForm: CreateSubscriptionFormRequestBody = {
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
    endpointAddressResolverMetadata: form.endpointAddressResolverMetadata,
    subscriptionIdentityHeadersEnabled: form.attachSubscriptionIdentityHeaders,
    autoDeleteWithTopicEnabled: form.deleteSubscriptionAutomatically,
  };

  if (isEndpointAnonymized(form.endpoint)) {
    delete parsedForm.endpoint;
  }

  return parsedForm;
}

function isEndpointAnonymized(endpoint: string) {
  const regexPattern: RegExp = /.*:([*]+)@.*/;
  return regexPattern.test(endpoint);
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
    backoffMaxIntervalInSec: parseFloat(
      String(form.subscriptionPolicy.backoffMaxIntervalInSec),
    ),
    backoffMultiplier: parseFloat(
      String(form.subscriptionPolicy.retryBackoffMultiplier),
    ),
    messageBackoff: parseFloat(String(form.subscriptionPolicy.retryBackoff)),
    messageTtl: parseFloat(String(form.subscriptionPolicy.inflightMessageTTL)),
    inflightSize: parseFloat(
      String(form.subscriptionPolicy.inflightMessagesCount),
    ),
    rate: parseFloat(String(form.subscriptionPolicy.rateLimit!!)),
    requestTimeout: parseFloat(String(form.subscriptionPolicy.requestTimeout)),
    sendingDelay: parseFloat(String(form.subscriptionPolicy.sendingDelay)),
    retryClientErrors: form.retryOn4xx,
  };
}

function mapBatchSerialSubscriptionPolicy(
  form: SubscriptionForm,
): BatchSubscriptionPolicyJson {
  return {
    messageTtl: parseFloat(String(form.subscriptionPolicy.inflightMessageTTL)),
    retryClientsErrors: form.retryOn4xx,
    messageBackoff: parseFloat(String(form.subscriptionPolicy.retryBackoff)),
    requestTimeout: parseFloat(String(form.subscriptionPolicy.requestTimeout)),
    batchSize: parseFloat(String(form.subscriptionPolicy.batchSize!!)),
    batchTime: parseFloat(String(form.subscriptionPolicy.batchTime!!)),
    batchVolume: parseFloat(String(form.subscriptionPolicy.batchVolume!!)),
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
