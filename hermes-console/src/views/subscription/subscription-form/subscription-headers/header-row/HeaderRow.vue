<script setup lang="ts">
  import TextField from '@/components/text-field/TextField.vue';

  const props = defineProps<{
    type: 'created' | 'new';
    name?: string;
    value?: string;
  }>();

  const emit = defineEmits(['add', 'remove', 'update:name', 'update:value']);
</script>

<template>
  <v-row v-if="props.type === 'created'">
    <v-col cols="5">
      <text-field
        :model-value="props.name"
        @input="emit('update:name', $event.target.value)"
        label="Name"
        placeholder="Header name"
      />
    </v-col>

    <v-col cols="5">
      <text-field
        :model-value="props.value"
        @input="emit('update:value', $event.target.value)"
        label="Value"
        placeholder="Header value"
      />
    </v-col>

    <v-col>
      <v-btn
        :ripple="false"
        variant="text"
        icon="mdi-delete"
        color="error"
        @click="emit('remove')"
      />
    </v-col>
  </v-row>
  <v-row v-else justify="end">
    <v-btn @click="emit('add')" :ripple="false" variant="text" color="success">
      Add Header
    </v-btn>
  </v-row>
</template>

<style scoped lang="scss"></style>
