<script setup lang="ts">
  import SelectField from '@/components/select-field/SelectField.vue';
  import TextField from '@/components/text-field/TextField.vue';
  import type { FilterMatchingStrategy } from '@/views/subscription/subscription-form/subscription-basic-filters/types';

  const props = defineProps<{
    type: 'created' | 'new';
    path?: string;
    matcher?: string;
    matchingStrategy?: FilterMatchingStrategy;
  }>();

  defineEmits([
    'add',
    'remove',
    'update:path',
    'update:matcher',
    'update:matchingStrategy',
  ]);
</script>

<template>
  <v-row class="mt-0">
    <v-col>
      <text-field
        :model-value="props.path"
        @input="$emit('update:path', $event.target.value)"
        label="Path"
        placeholder="Path for filters"
      />
    </v-col>

    <v-col>
      <text-field
        :model-value="props.matcher"
        @input="$emit('update:matcher', $event.target.value)"
        label="Matcher"
        placeholder="Matcher for filter"
      />
    </v-col>

    <v-col>
      <select-field
        :model-value="props.matchingStrategy"
        @update:model-value="$emit('update:matchingStrategy', $event)"
        label="Matching strategy"
        :items="['all', 'any']"
      />
    </v-col>

    <v-col cols="1">
      <v-btn
        v-if="props.type === 'new'"
        :ripple="false"
        variant="text"
        icon="mdi-plus-circle"
        color="success"
        @click="$emit('add')"
      />
      <v-btn
        v-else
        :ripple="false"
        variant="text"
        icon="mdi-delete"
        color="error"
        @click="$emit('remove')"
      />
    </v-col>
  </v-row>
</template>

<style scoped lang="scss"></style>
