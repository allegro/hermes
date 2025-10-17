<script setup lang="ts">
  import { ref } from 'vue';
  import { useAppConfigStore } from '@/store/app-config/useAppConfigStore';
  import ConsoleAlert from '@/components/console-alert/ConsoleAlert.vue';

  const props = defineProps<{
    actionButtonEnabled: boolean;
    title: string;
    text: string;
  }>();

  const configStore = useAppConfigStore();

  const confirmationText = ref<string>();
</script>

<template>
  <v-dialog width="100%" min-width="30%">
    <v-form @submit.prevent>
      <v-card>
        <v-card-item class="border-b">
          <v-card-title v-if="title" class="text-wrap">
            <v-avatar variant="tonal" color="error" start>
              <v-icon color="error" size="24">mdi-alert</v-icon>
            </v-avatar>
            {{ props.title }}
          </v-card-title>
        </v-card-item>

        <v-card-text class="pt-4">
          <span class="text-body-1">{{ props.text }}</span>
        </v-card-text>
        <v-card-text
          v-if="configStore.loadedConfig.console.criticalEnvironment"
        >
          <v-text-field
            :label="$t('confirmationDialog.confirmText')"
            type="input"
            v-model="confirmationText"
            prepend-inner-icon="mdi-alert"
          />
        </v-card-text>

        <v-card-actions>
          <v-col class="d-flex column-gap-2 justify-end">
            <v-btn
              variant="flat"
              color="error"
              @click="$emit('action')"
              :disabled="
                (configStore.loadedConfig.console.criticalEnvironment &&
                  confirmationText !== 'prod') ||
                !actionButtonEnabled
              "
            >
              {{ $t('confirmationDialog.confirm') }}
            </v-btn>
            <v-btn variant="flat" @click="$emit('cancel')">
              {{ $t('confirmationDialog.cancel') }}
            </v-btn>
          </v-col>
        </v-card-actions>
      </v-card>
    </v-form>
  </v-dialog>
</template>

<style scoped></style>
