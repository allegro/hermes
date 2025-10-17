<script setup lang="ts">
  const props = defineProps<{
    json: string;
  }>();
</script>

<template>
  <div>
    <pre class="raw-schema-snippet pa-3 bg-grey-lighten-5">{{
      JSON.parse(props.json)
    }}</pre>
  </div>
</template>

<style scoped lang="scss">
  .raw-schema-snippet {
    line-height: 1.2;
    font-size: 0.9rem;
    max-height: 500px;
    overflow: auto;
    cursor: text;
  }
</style>
