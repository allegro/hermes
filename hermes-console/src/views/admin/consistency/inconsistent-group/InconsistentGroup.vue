<script setup lang="ts">
  import { useConsistencyStore } from '@/store/consistency/useConsistencyStore';
  import { useI18n } from 'vue-i18n';
  import { useRouter } from 'vue-router';
  import { useSync } from '@/composables/sync/use-sync/useSync';
  import InconsistentMetadata from '@/views/admin/consistency/inconsistent-metadata/InconsistentMetadata.vue';

  const router = useRouter();
  const { t } = useI18n();

  const { groupId } = router.currentRoute.value.params as Record<
    string,
    string
  >;

  const consistencyStore = useConsistencyStore();
  const { syncGroup } = useSync();

  const group = consistencyStore.group(groupId);

  function onTopicClick(topicName: string) {
    router.push({ path: `/ui/consistency/${groupId}/topics/${topicName}` });
  }

  const breadcrumbsItems = [
    {
      title: t('consistency.breadcrumbs.home'),
      href: '/',
    },
    {
      title: t('consistency.breadcrumbs.title'),
      href: '/ui/consistency',
    },
    {
      title: groupId,
    },
  ];

  async function sync(datacenter: string) {
    const succeeded = await syncGroup(groupId, datacenter);
    if (succeeded) {
      router.push('/ui/consistency');
    }
  }
</script>

<template>
  <v-container>
    <v-row dense>
      <v-col md="12">
        <v-breadcrumbs :items="breadcrumbsItems" density="compact" />
      </v-col>
    </v-row>
    <v-row dense>
      <v-col md="12">
        <h3 class="text-h3">
          {{ t('consistency.inconsistentGroup.title', { groupId }) }}
        </h3>
      </v-col>
    </v-row>
    <InconsistentMetadata
      :metadata="group.inconsistentMetadata"
      v-if="group"
      @sync="sync"
      class="mt-8"
    ></InconsistentMetadata>
    <v-card
      class="mt-8 mb-2"
      v-if="group"
      :title="t('consistency.inconsistentGroup.listing.title')"
    >
      <v-table density="comfortable" hover>
        <thead>
          <tr>
            <th>{{ $t('consistency.inconsistentGroup.listing.index') }}</th>
            <th>{{ $t('consistency.inconsistentGroup.listing.name') }}</th>
          </tr>
        </thead>
        <tbody v-if="group.inconsistentTopics.length > 0">
          <tr
            v-for="(topic, index) in group.inconsistentTopics"
            :key="topic"
            @click="onTopicClick(topic.name)"
          >
            <td class="text-medium-emphasis">
              {{ index + 1 }}
            </td>
            <td class="font-weight-medium">
              {{ topic.name }}
            </td>
          </tr>
        </tbody>
        <tbody v-else>
          <tr>
            <th colspan="3" class="text-center text-medium-emphasis">
              {{ $t('consistency.inconsistentGroup.noTopics') }}
            </th>
          </tr>
        </tbody>
      </v-table>
    </v-card>
  </v-container>
</template>
