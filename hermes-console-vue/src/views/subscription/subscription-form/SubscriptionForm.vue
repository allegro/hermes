<script setup lang="ts">
  import { useCreateSubscription } from '@/composables/subscription/use-create-subscription/useCreateSubscription';
  import SelectField from '@/views/subscription/subscription-form/select-field/SelectField.vue';
  import TextField from '@/views/subscription/subscription-form/text-field/TextField.vue';

  const props = defineProps<{
    operation: 'add' | 'edit';
  }>();
  console.log(props);
  const { form, validators, dataSources } = useCreateSubscription();
</script>

<template>
  <v-form @submit.prevent class="d-flex flex-column row-gap-2">
    <text-field
      v-if="operation === 'add'"
      v-model="form.name"
      :rules="validators.name"
      label="Name"
      placeholder="Name of the subscription"
      :autofocus="true"
    />

    <text-field
      v-model="form.endpoint"
      :rules="validators.endpoint"
      label="Endpoint"
      placeholder="Where to send messages"
    />

    <text-field
      v-model="form.description"
      :rules="validators.description"
      label="Description"
      placeholder="Who and why subscribes?"
    />

    <div class="d-flex flex-row column-gap-2">
      <select-field
        v-model="form.ownerSource"
        :rules="validators.ownerSource"
        label="Owner source"
        :items="dataSources.ownerSources.value"
        class="w-33"
      />

      <v-autocomplete
        v-if="form.ownerSource?.autocomplete"
        v-model="form.owner"
        v-model:search="form.ownerSearch"
        :loading="dataSources.loadingOwners.value"
        :rules="validators.owner"
        label="Owner"
        :items="dataSources.owners.value"
        density="comfortable"
        class="w-66"
        variant="outlined"
        persistent-placeholder
      />

      <text-field
        v-else
        v-model="form.owner"
        :rules="validators.owner"
        label="Owner"
        class="w-66"
      />
    </div>

    <select-field
      v-model="form.deliveryType"
      :rules="validators.deliveryType"
      label="Delivery type"
      :items="dataSources.deliveryTypes"
    />

    <select-field
      v-model="form.contentType"
      :rules="validators.contentType"
      label="Content type"
      :items="dataSources.contentTypes.value"
    />

    <select-field label="Mode" :items="['ANYCAST', 'BROADCAST']" />
    <text-field
      type="number"
      label="Rate limit"
      suffix="messages/second"
      v-model="form.rateLimit"
    />
    <text-field
      type="number"
      label="Request timeout"
      suffix="milliseconds"
      v-model="form.subscriptionPolicy.requestTimeout"
    />
    <text-field
      type="number"
      label="Sending delay"
      suffix="milliseconds"
      v-model="form.subscriptionPolicy.sendingDelay"
    />
    <text-field
      type="number"
      label="Inflight message TTL"
      suffix="seconds"
      v-model="form.subscriptionPolicy.inflightMessageTTL"
    />

    <v-divider />

    <v-switch
      label="Retry on http 4xx status"
      inset
      color="success"
      density="comfortable"
      hide-details
      class="mt-1"
    />
    <text-field
      type="number"
      label="Retry backoff"
      suffix="milliseconds"
      class="mt-3"
      v-model="form.subscriptionPolicy.retryBackoff"
    />
    <text-field
      type="number"
      label="Retry backoff multiplier"
      v-model="form.subscriptionPolicy.retryBackoffMultiplier"
    />

    <v-divider />

    <select-field
      v-model="form.messageDeliveryTrackingMode"
      label="Message delivery tracking mode"
      :items="dataSources.messageDeliveryTrackingModes"
      class="mt-5"
    />
    <select-field
      v-model="form.monitoringDetails.severity"
      label="Monitoring severity"
      :items="dataSources.monitoringSeverities"
    />
    <text-field
      v-model="form.monitoringDetails.reaction"
      label="Monitoring reaction"
      placeholder="information for monitoring how to react when the subscription becomes unhealthy (e.g. team name or Pager Duty ID)"
    />
    <v-switch
      v-model="form.deliverUsingHttp2"
      label="Deliver using http/2"
      inset
      color="success"
      density="comfortable"
      hide-details
    />
    <v-switch
      v-model="form.attachSubscriptionIdentityHeaders"
      label="Attach subscription identity headers"
      inset
      color="success"
      density="comfortable"
      hide-details
    />
    <v-switch
      v-model="form.deleteSubscriptionAutomatically"
      label="Delete the subscription automatically"
      inset
      color="success"
      density="comfortable"
      hide-details
    />
  </v-form>
</template>

<style scoped lang="scss"></style>
