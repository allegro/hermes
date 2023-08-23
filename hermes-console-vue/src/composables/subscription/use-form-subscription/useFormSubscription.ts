import { computed, ref } from 'vue';
import { fetchOwnersSources } from '@/api/hermes-client';
import { matchRegex, max, min, required } from '@/utils/validators';
import { useAppConfigStore } from '@/store/app-config/useAppConfigStore';
import type {
  DataSources,
  FormValidators,
  SubscriptionForm,
} from '@/composables/subscription/use-form-subscription/types';
import type { OwnerSource } from '@/api/owner';
import type { Ref } from 'vue';
import type { SelectFieldOption } from '@/components/select-field/types';

export function useFormSubscription(initialFormState: SubscriptionForm) {
  const form = ref(initialFormState);
  const validators = formValidators(form);

  return {
    form,
    validators,
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
      max(form.value.deliveryType === 'SERIAL' ? 10000 : 1000000),
    ],
    sendingDelay: [required(), min(0), max(5000)],
    inflightMessageTTL: [required(), min(0), max(7200)],
    retryBackoff: [required(), min(0), max(1000000)],
    retryBackoffMultiplier: [required(), min(1), max(10)],
    messageDeliveryTrackingMode: [required()],
    monitoringSeverity: [required()],
  };
}

export function getDataSources(): DataSources {
  const configStore = useAppConfigStore();
  const contentTypes = [
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
    contentTypes,
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
