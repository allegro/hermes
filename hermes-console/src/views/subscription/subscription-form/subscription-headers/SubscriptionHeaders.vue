<script setup lang="ts">
  import { v4 as generateUUID } from 'uuid';
  import { ref } from 'vue';
  import HeaderRow from '@/views/subscription/subscription-form/subscription-headers/header-row/HeaderRow.vue';
  import type { HeaderWithId } from '@/views/subscription/subscription-form/subscription-headers/types';

  const props = defineProps<{
    modelValue: HeaderWithId[];
  }>();
  const emit = defineEmits(['update:modelValue']);

  const newName = ref('');
  const newValue = ref('');
  function addHeader() {
    const newHeader = {
      id: generateUUID(),
      name: newName.value,
      value: newValue.value,
    };
    const updatedHeaders = props.modelValue.concat([newHeader]);
    newName.value = '';
    newValue.value = '';
    emit('update:modelValue', updatedHeaders);
  }

  function removeHeader(headerToRemove: HeaderWithId) {
    const updatedHeaders = props.modelValue.filter(
      (header) => header !== headerToRemove,
    );
    emit('update:modelValue', updatedHeaders);
  }
</script>
<template>
  <span class="text-subtitle-1 mb-2">Fixed HTTP headers</span>
  <header-row
    v-for="header in props.modelValue"
    :key="header.id"
    v-model:name="header.name"
    v-model:value="header.value"
    @remove="removeHeader(header)"
    type="created"
  />

  <header-row
    v-model:name="newName"
    v-model:value="newValue"
    @add="addHeader()"
    type="new"
    class="mb-3"
  />
</template>

<style scoped lang="scss"></style>
