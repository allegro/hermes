<script setup lang="ts">
  import TextField from '@/components/text-field/TextField.vue';

  const props = defineProps<{
    type: 'created' | 'new';
    header?: string;
    matcher?: string;
  }>();

  const emit = defineEmits([
    'add',
    'remove',
    'update:header',
    'update:matcher',
  ]);
</script>

<template>
  <v-row v-if="props.type === 'created'">
    <v-col cols="5">
      <text-field
        :model-value="props.header"
        @input="emit('update:header', $event.target.value)"
        label="Name"
        placeholder="Header name"
      />
    </v-col>

    <v-col cols="5">
      <text-field
        :model-value="props.matcher"
        @input="emit('update:matcher', $event.target.value)"
        label="Matcher"
        placeholder="Matcher for filter"
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
      Add Filter
    </v-btn>
  </v-row>
</template>

<style scoped lang="scss"></style>
