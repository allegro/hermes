<script setup lang="ts">
  const props = defineProps<{
    source: string;
  }>();
</script>

<template>
  <v-expansion-panels>
    <v-expansion-panel :title="$t('topicView.offlineClients.title')">
      <v-expansion-panel-text>
        <iframe
          height="400"
          style="border: none"
          width="100%"
          :src="props.source"
        >
        </iframe>
      </v-expansion-panel-text>
    </v-expansion-panel>
  </v-expansion-panels>
</template>

<style scoped lang="scss"></style>
