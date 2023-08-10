import { ref } from 'vue';
import type { Ref } from 'vue';

export interface UseCreateSubscription {
    fields: SubscriptionFormFields;
    nameField: Ref<string>;
}

export interface SubscriptionFormFields {
    nameField: Ref<string>;
    endpointField: Ref<string>;
    descriptionField: Ref<string>;
}

export function useCreateSubscription(): UseCreateSubscription {
    const nameField = ref('');
    const endpointField = ref('');
    const descriptionField = ref('');

    const fields = {
        nameField,
        endpointField,
        descriptionField,
    }

    return {
        fields,
        nameField
    }
}
