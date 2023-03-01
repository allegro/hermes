<script setup lang="ts">
  import { useRoute } from 'vue-router';
  import GroupTopicsBreadcrumbs from '@/views/group-topics/group-topics-breadcrumbs/GroupTopicsBreadcrumbs.vue';

  const route = useRoute();
  const params = route.params as Record<string, string>;
  const { groupId } = params;
</script>

<template>
  <v-container>
    <v-row dense>
      <v-col md="12">
        <group-topics-breadcrumbs :group-id="groupId" />
        <!--
        <loading-spinner />
        <console-alert
          :title="t('groups.connectionError.title')"
          :text="t('groups.connectionError.text')"
          type="error"
        />
        -->
      </v-col>
    </v-row>
    <v-row dense>
      <v-col md="12">
        <v-card density="compact">
          <v-card-item>
            <p class="text-overline">Group</p>
            <p class="text-h4 font-weight-bold mb-2">
              {{ groupId }}
            </p>
          </v-card-item>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<style scoped lang="scss"></style>
