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
        <v-card-title v-if="title" class="text-wrap">
          {{ props.title }}
        </v-card-title>
        <v-card-text v-if="text">
          <console-alert :text="props.text" type="warning" />
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
          <v-col class="text-right">
            <v-btn
              type="confirm"
              color="primary"
              @click="$emit('action')"
              :disabled="
                (configStore.loadedConfig.console.criticalEnvironment &&
                  confirmationText !== 'prod') ||
                !actionButtonEnabled
              "
            >
              {{ $t('confirmationDialog.confirm') }}
            </v-btn>
            <v-btn @click="$emit('cancel')">
              {{ $t('confirmationDialog.cancel') }}
            </v-btn>
          </v-col>
        </v-card-actions>
      </v-card>
    </v-form>
  </v-dialog>
</template>

<style scoped></style>
