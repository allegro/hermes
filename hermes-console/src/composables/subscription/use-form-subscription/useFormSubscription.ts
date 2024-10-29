import { computed, ref } from 'vue';
import { DeliveryType } from '@/api/subscription';
import { fetchOwnersSources, searchOwners } from '@/api/hermes-client';
import { v4 as generateUUID } from 'uuid';
import { matchRegex, max, min, required } from '@/utils/validators';
import { useAppConfigStore } from '@/store/app-config/useAppConfigStore';
import { watch } from 'vue';
import type {
  DataSources,
  FormValidators,
  RawDataSources,
  SubscriptionForm,
  UseFormSubscription,
} from '@/composables/subscription/use-form-subscription/types';
import type { EndpointAddressResolverMetadata } from '@/api/subscription';
import type { HeaderFilter } from '@/views/subscription/subscription-form/subscription-header-filters/types';
import type {
  MessageFilterSpecification,
  Subscription,
} from '@/api/subscription';
import type { OwnerSource } from '@/api/owner';
import type { PathFilter } from '@/views/subscription/subscription-form/subscription-basic-filters/types';
import type { Ref } from 'vue';
import type { SelectFieldOption } from '@/components/select-field/types';
import type { UseCreateSubscriptionErrors } from '@/composables/subscription/use-create-subscription/types';

export function useFormSubscription(): UseFormSubscription {
  const form = createEmptyForm();
  const validators = formValidators(form);
  const rawDataSources = getRawDataSources();
  const dataSources = {
    ...rawDataSources,
    contentTypes: computed(() =>
      rawDataSources.allContentTypes.filter(
        (contentType) =>
          !contentType.unsupportedDeliveryTypes.includes(
            form.value.deliveryType,
          ),
      ),
    ),
  };

  return {
    form,
    validators,
    dataSources,
  };
}

function formValidators(form: Ref<SubscriptionForm>): FormValidators {
  return {
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
    requestTimeout: [
      required(),
      min(0),
      max(form.value.deliveryType === DeliveryType.BATCH ? 1000000 : 10000),
    ],
    sendingDelay: [required(), min(0), max(5000)],
    inflightMessageTTL: [required(), min(0), max(7200)],
    inflightMessagesCount: [min(1)],
    retryBackoff: [required(), min(0), max(1000000)],
    retryBackoffMultiplier: [required(), min(1), max(10)],
    backoffMaxIntervalInSec: [required(), min(1), max(600)],
    messageDeliveryTrackingMode: [required()],
    monitoringSeverity: [required()],
  };
}

function getRawDataSources(): RawDataSources {
  const configStore = useAppConfigStore();
  const allContentTypes = [
    { title: 'JSON', value: 'JSON', unsupportedDeliveryTypes: [] },
    {
      title: 'AVRO (not supported in BATCH delivery mode)',
      value: 'AVRO',
      unsupportedDeliveryTypes: ['BATCH'],
    },
  ];
  const deliveryTypes = configStore.loadedConfig.subscription.deliveryTypes.map(
    (type) => {
      return {
        title: type.label,
        value: type.value,
      };
    },
  );
  const deliveryModes = [
    { title: 'ANYCAST', value: 'ANYCAST' },
    { title: 'BROADCAST (incubating feature)', value: 'BROADCAST' },
  ];
  const monitoringSeverities = [
    { title: 'Non-important', value: 'NON_IMPORTANT' },
    { title: 'Important', value: 'IMPORTANT' },
    { title: 'Critical', value: 'CRITICAL' },
  ];
  const messageDeliveryTrackingModes = [
    { title: 'No tracking', value: 'trackingOff' },
    { title: 'Track message discarding only', value: 'discardedOnly' },
    { title: 'Track everything', value: 'trackingAll' },
  ];
  const fetchedOwnerSources = ref<OwnerSource[]>([]);
  const ownerSources = computed(() =>
    fetchedOwnerSources.value
      .filter((source) => !source.deprecated)
      .map((source) => {
        return { title: source.name, value: source };
      }),
  );
  fetchOwnersSources().then(
    (response) => (fetchedOwnerSources.value = response.data),
  );
  const owners = ref<SelectFieldOption[]>([]);
  const loadingOwners = ref(false);

  return {
    allContentTypes,
    deliveryTypes,
    deliveryModes,
    monitoringSeverities,
    messageDeliveryTrackingModes,
    fetchedOwnerSources,
    ownerSources,
    owners,
    loadingOwners,
  };
}

function getEndpointAddressResolverDefaultValues(): Record<string, any> {
  const configStore = useAppConfigStore();
  const defaults: Record<string, any> = {};
  for (const [propertyName, value] of Object.entries(
    configStore.appConfig!.subscription.endpointAddressResolverMetadata,
  )) {
    if (value.type == 'boolean') {
      defaults[propertyName] = false;
    }
  }
  return defaults;
}

function getEndpointAddressResolverValues(
  metadata: EndpointAddressResolverMetadata,
): Record<string, any> {
  const mergedMetadata: Record<string, any> = {};
  for (const [propertyName, defaultValue] of Object.entries(
    getEndpointAddressResolverDefaultValues(),
  )) {
    if (metadata[propertyName] !== undefined) {
      mergedMetadata[propertyName] = metadata[propertyName];
    } else {
      mergedMetadata[propertyName] = defaultValue;
    }
  }
  return mergedMetadata;
}

function createEmptyForm(): Ref<SubscriptionForm> {
  return ref({
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
    endpointAddressResolverMetadata: getEndpointAddressResolverDefaultValues(),
  });
}

export function initializeFullyFilledForm(
  form: Ref<SubscriptionForm>,
  subscription: Subscription,
): void {
  form.value = {
    name: subscription.name,
    endpoint: subscription.endpoint,
    description: subscription.description,
    ownerSource: null,
    owner: subscription.owner.id,
    ownerSearch: '',
    contentType: subscription.contentType,
    deliveryType: subscription.deliveryType,
    mode: subscription.mode,
    subscriptionPolicy: {
      rateLimit:
        subscription.deliveryType === DeliveryType.SERIAL
          ? subscription.subscriptionPolicy.rate
          : null,
      inflightMessageTTL: subscription.subscriptionPolicy.messageTtl,
      inflightMessagesCount: subscription.subscriptionPolicy.inflightSize,
      retryBackoff: subscription.subscriptionPolicy.messageBackoff,
      retryBackoffMultiplier:
        subscription.deliveryType === DeliveryType.SERIAL
          ? subscription.subscriptionPolicy.backoffMultiplier
          : 1.0,
      backoffMaxIntervalInSec:
        subscription.deliveryType === DeliveryType.SERIAL
          ? subscription.subscriptionPolicy.backoffMaxIntervalInSec
          : 600,
      sendingDelay:
        subscription.deliveryType === DeliveryType.SERIAL
          ? subscription.subscriptionPolicy.sendingDelay
          : 0,
      requestTimeout: subscription.subscriptionPolicy.requestTimeout,
      batchSize:
        subscription.deliveryType === DeliveryType.BATCH
          ? subscription.subscriptionPolicy.batchSize
          : null,
      batchTime:
        subscription.deliveryType === DeliveryType.BATCH
          ? subscription.subscriptionPolicy.batchTime
          : null,
      batchVolume:
        subscription.deliveryType === DeliveryType.BATCH
          ? subscription.subscriptionPolicy.batchVolume
          : null,
    },
    retryOn4xx: subscription.subscriptionPolicy.retryClientErrors,
    messageDeliveryTrackingMode: subscription.trackingMode,
    monitoringDetails: {
      severity: subscription.monitoringDetails.severity,
      reaction: subscription.monitoringDetails.reaction,
    },
    deliverUsingHttp2: subscription.http2Enabled,
    attachSubscriptionIdentityHeaders:
      subscription.subscriptionIdentityHeadersEnabled,
    deleteSubscriptionAutomatically: subscription.autoDeleteWithTopicEnabled,
    pathFilters: mapToPathFilter(subscription.filters),
    headerFilters: mapToHeaderFilter(subscription.filters),
    endpointAddressResolverMetadata: getEndpointAddressResolverValues(
      subscription.endpointAddressResolverMetadata,
    ),
  };
}

function mapToHeaderFilter(
  filters: MessageFilterSpecification,
): HeaderFilter[] {
  return filters
    .filter((filter: MessageFilterSpecification) => filter.type === 'header')
    .map((filter: MessageFilterSpecification) => {
      return {
        id: generateUUID(),
        headerName: filter.header,
        matcher: filter.matcher,
      };
    });
}

function mapToPathFilter(filters: MessageFilterSpecification): PathFilter[] {
  return filters
    .filter((filter: MessageFilterSpecification) => filter.type !== 'header')
    .map((filter: MessageFilterSpecification) => {
      return {
        id: generateUUID(),
        path: filter.path,
        matcher: filter.matcher,
        matchingStrategy: filter.matchingStrategy,
      };
    });
}

export function watchOwnerSearch(
  form: Ref<SubscriptionForm>,
  dataSources: DataSources,
  errors: Ref<UseCreateSubscriptionErrors>,
) {
  const searchTimeout = ref();
  watch(
    () => form.value.ownerSearch,
    async (searchingPhrase) => {
      const selectedOwnerSource = form.value.ownerSource;
      if (!selectedOwnerSource || !searchingPhrase) {
        return;
      }
      clearTimeout(searchTimeout.value);
      searchTimeout.value = setTimeout(async () => {
        try {
          dataSources.loadingOwners.value = true;
          dataSources.owners.value = (
            await searchOwners(selectedOwnerSource.name, searchingPhrase)
          ).data.map((source) => {
            return { title: source.name, value: source.id };
          });
        } catch (e) {
          errors.value.fetchOwners = e as Error;
        } finally {
          dataSources.loadingOwners.value = false;
        }
      }, 500);
    },
  );
}
