<script setup lang="ts">
  const props = defineProps<{
    json: string;
  }>();
</script>

<template>
  <pre class="raw-schema-snippet pa-3" v-show="showRawSchema">{{
    JSON.parse(props.json)
  }}</pre>
</template>

<style scoped lang="scss">
  .raw-schema-snippet {
    line-height: 1.2;
    font-size: 0.9rem;
    max-height: 500px;
    overflow: scroll;
    cursor: text;
  }
</style>
