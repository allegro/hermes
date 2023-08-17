<script setup lang="ts">
  import { ref } from 'vue';
  import { required } from '@/utils/validators';
  import TextField from '@/components/text-field/TextField.vue';

  const props = defineProps<{
    type: 'created' | 'new';
    name?: string;
    matcher?: string;
  }>();
  const isFormValid = ref(false);

  const emit = defineEmits(['add', 'remove', 'update:name', 'update:matcher']);

  const submit = () => {
    if (props.type === 'created') {
      emit('remove');
    } else if (isFormValid.value) {
      emit('add');
    }
  };
</script>

<template>
  <v-form v-model="isFormValid" @submit.prevent="submit">
    <v-row>
      <v-col>
        <text-field
          :model-value="props.name"
          @input="$emit('update:name', $event.target.value)"
          label="Name"
          placeholder="Header name"
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
        <v-btn
          v-if="props.type === 'new'"
          type="submit"
          :ripple="false"
          variant="text"
          icon="mdi-plus-circle"
          color="success"
        />
        <v-btn
          v-else
          type="submit"
          :ripple="false"
          variant="text"
          icon="mdi-delete"
          color="error"
        />
      </v-col>
    </v-row>
  </v-form>
</template>

<style scoped lang="scss"></style>
