<script setup lang="ts">
import TextField from '@/views/subscription/subscription-form/text-field/TextField.vue';
import SelectField from '@/views/subscription/subscription-form/select-field/SelectField.vue';
import { useCreateSubscription } from '@/composables/subscription/use-create-subscription/useCreateSubscription';

const {
  fields,
  nameField
} = useCreateSubscription();

</script>

<template>
  <v-form @submit.prevent>
    {{ nameField }}
    <text-field v-model="fields.nameField.value" label="Name" placeholder="Name of the subscription" :autofocus="true"/>
    <text-field v-model="fields.endpointField" label="Endpoint" placeholder="Where to send messages"/>
    <text-field v-model="fields.descriptionField" label="Description" placeholder="Who and why subscribes?"/>

    <div class="d-flex flex-row column-gap-2">
      <select-field label="Owner source" :items="['Service Catalog']" class="w-33"/>
      <v-autocomplete
          label="Owner"
          :items="['service-a', 'service-b']"
          density="comfortable"
          class="w-66"
          variant="outlined"
          persistent-placeholder
      />
    </div>

    <select-field label="Content type" :items="['JSON', 'AVRO']"/>
    <select-field label="Delivery type" :items="['SERIAL', 'BATCH']"/>
    <select-field label="Mode" :items="['ANYCAST', 'BROADCAST']"/>
    <text-field label="Rate limit" suffix="messages/second"/>
    <text-field label="Request timeout" suffix="milliseconds"/>
    <text-field label="Sending delay" suffix="milliseconds"/>
    <text-field label="Inflight message TTL" suffix="seconds"/>

    <v-divider/>

    <v-switch label="Retry on http 4xx status" inset color="success" density="comfortable" hide-details class="mt-1"/>
    <text-field label="Retry backoff" suffix="milliseconds" class="mt-3"/>
    <text-field label="Retry backoff multiplier"/>

    <v-divider/>

    <select-field
        label="Message delivery tracking mode"
        :items="['No tracking', 'Track message discarding only', 'Track everything']"
        class="mt-5"
    />
    <select-field label="Monitoring severity" :items="[]"/>
    <select-field label="Monitoring reaction" :items="[]"/>
    <v-switch label="Deliver using http/2" inset color="success" density="comfortable" hide-details/>
    <v-switch label="Attach subscription identity headers" inset color="success" density="comfortable" hide-details/>
    <v-switch label="Delete the subscription automatically" inset color="success" density="comfortable" hide-details/>
  </v-form>
</template>

<style scoped lang="scss">

</style>
