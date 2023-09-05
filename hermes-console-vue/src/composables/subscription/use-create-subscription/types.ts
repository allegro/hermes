import type {
  DataSources,
  FormValidators,
  SubscriptionForm,
} from '@/composables/subscription/use-form-subscription/types';
import type { Ref } from 'vue';

export interface UseCreateSubscription {
  form: Ref<SubscriptionForm>;
  validators: FormValidators;
  dataSources: DataSources;
  creatingSubscription: Ref<boolean>;
  errors: Ref<UseCreateSubscriptionErrors>;
  createSubscription: () => Promise<boolean>;
}

export interface UseCreateSubscriptionErrors {
  fetchOwnerSources: Error | null;
  fetchOwners: Error | null;
}
