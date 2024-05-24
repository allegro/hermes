import { fetchOwner } from '@/api/hermes-client';
import { initializeFullyFilledForm } from '@/composables/subscription/use-form-subscription/useFormSubscription';
import type {
  DataSources,
  SubscriptionForm,
} from '@/composables/subscription/use-form-subscription/types';
import type { Ref } from 'vue';

export interface UseImportSubscription {
  importFormData: (
    importedFile: Ref<any>,
    form: Ref<SubscriptionForm>,
    dataSources: DataSources,
  ) => void;
}

export function useImportSubscription(): UseImportSubscription {
  function importFormData(
    importedFile: Ref<any>,
    form: Ref<SubscriptionForm>,
    dataSources: DataSources,
  ) {
    if (importedFile.value) {
      const reader = new FileReader();
      reader.readAsText(importedFile.value);

      reader.onload = function () {
        const subscription = JSON.parse(<string>reader.result);
        initializeFullyFilledForm(form, subscription);
        form.value.ownerSource = dataSources.ownerSources.value.find(
          (ownerSource: { value: { name: any } }) =>
            ownerSource.value.name === subscription.owner.source,
        )?.value!!;
        fetchOwner(subscription.owner.id, subscription.owner.source)
          .then((owner) => owner.data)
          .then(
            (owner) =>
              (dataSources.owners.value = [
                { title: owner.name, value: owner.id },
              ]),
          );
      };
    }
  }

  return {
    importFormData,
  };
}
