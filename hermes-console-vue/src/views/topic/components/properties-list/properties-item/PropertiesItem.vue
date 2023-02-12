<script setup lang="ts">
interface Props {
  name: string;
  value: any;
}

const { name, value } = defineProps<Props>();
</script>

<template>
  <div>
    <span class="font-weight-bold">{{ name }}: </span>
    <span>{{ value }}</span>
  </div>
</template>

<style scoped lang="scss">

</style>
