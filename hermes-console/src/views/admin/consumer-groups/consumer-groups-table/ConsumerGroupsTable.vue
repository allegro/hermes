<script setup lang="ts">
  import ConsumerGroupMembers from '@/views/admin/consumer-groups/consumer-group-member/ConsumerGroupMembers.vue';
  import type { ConsumerGroup } from '@/api/consumer-group';

  const props = defineProps<{
    consumerGroups: ConsumerGroup[];
  }>();
</script>

<template>
  <v-row dense v-if="props.consumerGroups">
    <v-col
      md="6"
      v-for="consumerGroup in props.consumerGroups"
      :key="consumerGroup"
    >
      <v-card density="compact" v-if="consumerGroup.state === 'Stable'">
        <v-card-item>
          <p class="text-h5 font-weight-bold text-green">
            {{ consumerGroup.clusterName }} ({{ consumerGroup.state }})
          </p>
        </v-card-item>
      </v-card>
      <v-card v-else-if="consumerGroup.state === 'Dead'" density="compact">
        <v-card-item>
          <p class="text-h5 font-weight-bold text-red">
            {{ consumerGroup.clusterName }} ({{ consumerGroup.state }})
          </p>
        </v-card-item>
      </v-card>
      <v-card v-else>
        <v-card-item>
          <p class="text-h5 font-weight-bold text-orange">
            {{ consumerGroup.clusterName }} ({{ consumerGroup.state }})
          </p>
        </v-card-item>
      </v-card>
      <consumer-group-members :members="consumerGroup.members" />
    </v-col>
  </v-row>
</template>

<style scoped lang="scss"></style>
