import type { ComputedRef, Ref } from 'vue';
import type {
  FormValidators,
  SubscriptionForm,
} from '@/composables/subscription/use-form-subscription/types';
import type { OwnerSource } from '@/api/owner';
import type { SelectFieldOption } from '@/components/select-field/types';

export interface UseCreateSubscription {
  form: Ref<SubscriptionForm>;
  validators: FormValidators;
  dataSources: DataSources;
  creatingSubscription: Ref<boolean>;
  errors: Ref<UseCreateSubscriptionErrors>;
  createSubscription: () => Promise<void>;
}

export interface UseCreateSubscriptionErrors {
  fetchOwnerSources: Error | null;
  fetchOwners: Error | null;
}

export interface DataSources {
  contentTypes: ComputedRef<SelectFieldOption[]>;
  deliveryTypes: SelectFieldOption[];
  deliveryModes: SelectFieldOption[];
  monitoringSeverities: SelectFieldOption[];
  messageDeliveryTrackingModes: SelectFieldOption[];
  ownerSources: ComputedRef<SelectFieldOption<OwnerSource>[]>;
  owners: Ref<SelectFieldOption[]>;
  loadingOwners: Ref<boolean>;
}
