import type {
  DataSources,
  FormValidators,
  TopicForm,
} from '@/composables/topic/use-form-topic/types';
import type { Ref } from 'vue';

export interface UseEditTopic {
  form: Ref<TopicForm>;
  validators: FormValidators;
  dataSources: DataSources;
  creatingOrUpdatingTopic: Ref<boolean>;
  errors: Ref<UseEditTopicErrors>;
  createOrUpdateTopic: () => Promise<boolean>;
}

export interface UseEditTopicErrors {
  fetchOwnerSources: Error | null;
  fetchOwners: Error | null;
}
