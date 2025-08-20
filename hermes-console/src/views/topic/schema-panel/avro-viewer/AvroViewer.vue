<script lang="ts" setup>
  import AvroNode from '@/views/topic/schema-panel/avro-viewer/AvroNode.vue';
  import type { AvroSchema } from '@/views/topic/schema-panel/AvroTypes';
  const props = defineProps<{
    schema: string;
  }>();

  const rootField = (): AvroSchema => {
    const jsonSchema: AvroSchema = JSON.parse(props.schema);
    return {
      name: jsonSchema.name,
      doc: jsonSchema.doc,
      type: {
        type: 'record',
        fields: jsonSchema.fields?.filter(
          (field) => field.name !== '__metadata',
        ),
      },
    };
  };
</script>

<template>
  <v-card class="px-3 py-3">
    <AvroNode v-if="rootField()" :field="rootField()" :root="true" />
  </v-card>
</template>

<style lang="scss" scoped>
  .v-theme--dark .avro-schema {
    background-color: #343434;
  }
</style>
