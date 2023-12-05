import type {
  DataSources,
  FormValidators,
  SubscriptionForm,
} from '@/composables/subscription/use-form-subscription/types';
import type { Ref } from 'vue';
import type { UseCreateSubscriptionErrors } from '@/composables/subscription/use-create-subscription/types';

export interface UseEditSubscription {
  form: Ref<SubscriptionForm>;
  validators: FormValidators;
  dataSources: DataSources;
  creatingOrUpdatingSubscription: Ref<boolean>;
  errors: Ref<UseCreateSubscriptionErrors>;
  createOrUpdateSubscription: () => Promise<boolean>;
}

export interface UseEditSubscriptionErrors {}
