<script setup lang="ts">
  import { isAny } from '@/utils/roles-util';
  import { ref } from 'vue';
  import { Role } from '@/api/role';
  import { State } from '@/api/subscription';
  import { useI18n } from 'vue-i18n';
  import { useRouter } from 'vue-router';
  import SubscriptionForm from '@/views/subscription/subscription-form/SubscriptionForm.vue';
  import type { Subscription } from '@/api/subscription';

  const router = useRouter();
  const filter = ref<string>('');

  const props = defineProps<{
    groupId: string;
    topicName: string;
    subscriptions: Subscription[];
    roles: Role[] | undefined;
  }>();

  const { t } = useI18n();

  const emit = defineEmits<{
    copyClientsClick: [];
  }>();

  const statusTextColor: Record<State, string> = {
    [State.ACTIVE]: 'green',
    [State.PENDING]: 'orange',
    [State.SUSPENDED]: 'error',
  };

  const subscriptionTableHeaders = [
    { key: 'name', title: t('topicView.subscriptions.tableHeaders.name') },
  ];

  const showSubscriptionCreationForm = ref(false);

  function showSubscriptionForm() {
    showSubscriptionCreationForm.value = true;
  }

  function hideSubscriptionForm() {
    showSubscriptionCreationForm.value = false;
  }

  function pushToSubscription(subscription: string) {
    router.push({
      path: `/ui/groups/${props.groupId}/topics/${props.topicName}/subscriptions/${subscription}`,
    });
  }
</script>

<template>
  <div class="d-flex flex-column row-gap-2">
    <div class="d-flex justify-space-between">
      <v-dialog
        v-model="showSubscriptionCreationForm"
        min-width="800"
        :persistent="true"
      >
        <template #activator>
          <div>
            <v-text-field
              variant="outlined"
              base-color="rgba(var(--v-border-color), 0.31)"
              single-line
              :label="$t('topicView.subscriptions.search')"
              density="compact"
              v-model="filter"
              prepend-inner-icon="mdi-magnify"
              hide-details
              style="min-width: 300px"
            />
          </div>
          <div>
            <v-btn
              v-if="subscriptions.length > 0"
              :disabled="!isAny(roles)"
              prepend-icon="mdi-content-copy"
              @click="emit('copyClientsClick')"
              style="margin-right: 10px"
              variant="outlined"
              class="text-capitalize"
            >
              {{ $t('topicView.subscriptions.copy') }}
            </v-btn>
            <v-btn
              :disabled="!isAny(roles)"
              prepend-icon="mdi-plus"
              variant="outlined"
              @click="showSubscriptionForm()"
              class="text-capitalize"
              >{{ $t('topicView.subscriptions.create') }}
            </v-btn>
          </div>
        </template>
        <v-card>
          <v-card-item class="border-b">
            <div class="d-flex justify-space-between align-center">
              <v-card-title>
                {{ $t('topicView.subscriptions.create') }}
              </v-card-title>
              <v-btn
                icon="mdi-close"
                variant="text"
                @click="hideSubscriptionForm"
              />
            </div>
          </v-card-item>

          <v-card-text class="pt-4">
            <SubscriptionForm
              operation="add"
              :subscription="null"
              :topic="props.topicName"
              :roles="roles"
              @created="pushToSubscription"
              @cancel="hideSubscriptionForm"
            />
          </v-card-text>
        </v-card>
      </v-dialog>
    </div>

    <v-data-table
      :items="props.subscriptions"
      :search="filter"
      :headers="subscriptionTableHeaders"
      hover
      :items-per-page="-1"
    >
      <template #item="{ item }">
        <v-list-item
          :href="`/ui/groups/${props.groupId}/topics/${props.topicName}/subscriptions/${item.name}`"
        >
          <div class="d-flex justify-space-between align-center">
            <div>{{ item.name }}</div>
            <div>
              <v-chip size="small" :color="statusTextColor[item.state]"
                >{{ item.state }}
              </v-chip>
            </div>
          </div>
        </v-list-item>
      </template>
    </v-data-table>
  </div>
</template>
