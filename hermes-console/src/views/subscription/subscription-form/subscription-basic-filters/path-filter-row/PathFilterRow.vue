<script setup lang="ts">
  import SelectField from '@/components/select-field/SelectField.vue';
  import TextField from '@/components/text-field/TextField.vue';
  import type { FilterMatchingStrategy } from '@/views/subscription/subscription-form/subscription-basic-filters/types';

  const props = defineProps<{
    type: 'created' | 'new';
    path?: string;
    matcher?: string;
    matchingStrategy?: FilterMatchingStrategy;
    paths?: string[];
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
    <v-col cols="5">
      <v-combobox
        :items="paths"
        :model-value="props.path"
        :persistent-placeholder="true"
        density="comfortable"
        label="Path"
        placeholder="Path for filters"
        variant="outlined"
        @update:modelValue="$emit('update:path', $event)"
      ></v-combobox>
    </v-col>

    <v-col cols="3">
      <text-field
        :model-value="props.matcher"
        label="Matcher"
        placeholder="Matcher for filter"
        @input="$emit('update:matcher', $event.target.value)"
      />
    </v-col>

    <v-col cols="3">
      <select-field
        :items="['all', 'any']"
        :model-value="props.matchingStrategy"
        label="Matching strategy"
        @update:model-value="$emit('update:matchingStrategy', $event)"
      />
    </v-col>

    <v-col cols="1">
      <v-btn
        v-if="props.type === 'new'"
        :ripple="false"
        color="success"
        icon="mdi-plus-circle"
        variant="text"
        @click="$emit('add')"
      />
      <v-btn
        v-else
        :ripple="false"
        color="error"
        icon="mdi-delete"
        variant="text"
        @click="$emit('remove')"
      />
    </v-col>
  </v-row>
</template>

<style scoped lang="scss"></style>
