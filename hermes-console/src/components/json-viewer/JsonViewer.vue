<script setup lang="ts">
  const props = defineProps<{
    json: string;
  }>();
</script>

<template>
  <div>
    <pre class="raw-schema-snippet pa-3">{{ JSON.parse(props.json) }}</pre>
  </div>
</template>

<style scoped lang="scss">
  @use 'vuetify/settings';

  .raw-schema-snippet {
    font-size: settings.$code-font-size;
    line-height: 1.2;
    max-height: 500px;
    overflow: auto;
    cursor: text;
    color: settings.$code-color;
    background-color: settings.$code-background-color;
  }
</style>
