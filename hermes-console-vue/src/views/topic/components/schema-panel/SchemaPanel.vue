<script setup lang="ts">
  const props = defineProps<{
    schema: string;
  }>();

  const niceJson = JSON.parse(props.schema);
</script>

<template>
  <v-expansion-panels>
    <v-expansion-panel title="Message schema">
      <v-expansion-panel-text>
        <pre>
          <v-code class="raw-schema-snippet">{{ niceJson }}</v-code>
        </pre>
      </v-expansion-panel-text>
    </v-expansion-panel>
  </v-expansion-panels>
</template>

<style scoped lang="scss">
  .raw-schema-snippet {
    line-height: 1.4;
    max-height: 500px;
    overflow-y: scroll;
    overflow-x: scroll;
    border: #cccccc 1px solid;
  }
</style>
