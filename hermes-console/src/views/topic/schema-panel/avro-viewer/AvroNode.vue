<script lang="ts" setup>
  import { defineProps, ref } from 'vue';
  import type { AvroSchema, Type } from '@/views/topic/schema-panel/AvroTypes';

  const props = defineProps<{
    field: AvroSchema;
    root: boolean;
  }>();
  const expanded = ref(true);

  const getComplexType = (type: Type) => {
    if (type.type === 'array') {
      const arrayType = getTypes(type.items)[0];
      return `${arrayType}[]`;
    } else if (type.type === 'map') {
      const mapType = getTypes(type.values)[0];
      return `map[string]${mapType}`;
    } else if (type.type === 'fixed') {
      return `fixed(${type.size}B)`;
    }
    return type.type;
  };

  const getTypes = (type: Type): Type => {
    if (typeof type === 'string') {
      return [type.toLowerCase()];
    } else if (Array.isArray(type)) {
      let types = type.flatMap((type) => getTypes(type));
      const nullable = types.includes('null');
      if (nullable) {
        types = types
          .filter((type) => type !== 'null')
          .map((type) => `${type}?`);
      }
      return types;
    } else if (Object.getOwnPropertyDescriptor(type, 'type')) {
      return [getComplexType(type)];
    }
    return ['unknown'];
  };

  const findNestedType = (type: Type): Type => {
    if (Array.isArray(type)) {
      return type
        .map((subType) => findNestedType(subType))
        .find((subType) => Object.getOwnPropertyDescriptor(subType, 'type'));
    }
    if (type.type === 'array') return findNestedType(type.items);
    if (type.type === 'map') return type.values;
    return type;
  };

  const findEnumSymbols = (type: Type) => {
    if (Array.isArray(type)) {
      return type.find((subType) =>
        Object.getOwnPropertyDescriptor(subType, 'type'),
      ).symbols;
    }
    if (type.type === 'enum') return type.symbols;
    if (type.type === 'array') return type.items.symbols;
    if (type.type === 'map') return type.values.symbols;
  };

  const types = getTypes(props.field.type);
  const isRecord = types.some((type: Type) => type.includes('record'));
  const isEnum = types.some((type: Type) => type.includes('enum'));
  const expandable = isRecord || isEnum;
  const nestedType = isRecord && findNestedType(props.field.type);
  const enumSymbols = isEnum && findEnumSymbols(props.field.type);

  const toggle = (event: Event) => {
    event.stopPropagation();
    expanded.value = !expanded.value;
  };
</script>

<template>
  <div
    :style="{ cursor: expandable ? 'pointer' : 'default' }"
    class="flex-row"
    @click="toggle"
  >
    <div v-if="!root" class="tree-branch" />
    <v-icon
      :icon="expanded ? 'mdi-menu-down' : 'mdi-menu-right'"
      :style="{ visibility: expandable ? 'visible' : 'hidden' }"
    ></v-icon>
    <span class="text-blue">{{ props.field.name }}</span>
    <span v-if="!root" class="type">&nbsp;{{ types.join('|') }}</span>
    <span v-if="props.field.doc" class="text-grey">
      &nbsp; {{ props.field.doc }}
    </span>
    <span v-if="props.field.default">
      {{ $t('topicView.schema.default') }}: {{ props.field.default }}
    </span>
  </div>
  <div v-show="expanded" style="padding-left: 2em" @click="toggle">
    <div v-if="isRecord">
      <AvroNode
        v-for="nestedField in nestedType.fields"
        :key="nestedField.name"
        :field="nestedField"
        :root="false"
      />
    </div>
    <div v-if="isEnum">
      <div class="tree-branch"></div>
      <span v-for="symbol in enumSymbols" :key="symbol">
        <code class="enum">{{ symbol }}</code>
        <code v-if="symbol != enumSymbols[enumSymbols.length - 1]">, </code>
      </span>
    </div>
  </div>
</template>

<style lang="scss" scoped>
  .tree-branch {
    position: absolute;
    border-bottom: 1px dashed rgba(0, 0, 0, 0.2);
    border-left: 1px dashed rgba(0, 0, 0, 0.2);
    cursor: default;
    margin-left: -1.5em;
    height: 1em;
    width: 1em;
  }
  .enum {
    padding: 2px 4px;
    font-size: 90%;
    color: #c7254e;
    background-color: #f9f2f4;
    border-radius: 5px;
  }
  .type {
    color: #144c71;
    font-family: monospace;
  }
  .v-theme--dark {
    .tree-branch {
      border-bottom: 1px dashed #d3d3d3;
      border-left: 1px dashed #d3d3d3;
    }
    .enum {
      color: #d3d3d3;
      background-color: #632420;
    }
    .type {
      color: #3285bb;
    }
  }
</style>
