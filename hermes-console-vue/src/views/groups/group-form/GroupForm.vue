<script setup lang="ts">
  import { useI18n } from 'vue-i18n';
  import ConsoleAlert from '@/components/console-alert/ConsoleAlert.vue';

  const props = defineProps<{
    dialogOpen: boolean;
    operation: 'create' | 'edit';
  }>();

  const emit = defineEmits<{
    (e: 'update:dialogOpen', dialog: boolean): void;
  }>();

  const { t } = useI18n();

  const groupNameRules = [
    (value: string) => {
      if (value) return true;
      return t('groups.groupForm.validation.groupName');
    },
  ];
</script>

<template>
  <v-dialog
    :model-value="props.dialogOpen"
    @update:model-value="(value) => emit('update:dialogOpen', value)"
    width="100%"
    min-width="30%"
  >
    <v-form @submit.prevent>
      <v-card>
        <v-card-title>
          {{
            props.operation === 'create'
              ? t('groups.groupForm.createTitle')
              : t('groups.groupForm.editTitle')
          }}
        </v-card-title>
        <v-card-item v-if="props.operation === 'create'">
          <console-alert :text="t('groups.groupForm.edu')" type="warning" />
        </v-card-item>
        <v-card-item>
          <v-text-field
            :label="t('groups.groupForm.groupName')"
            :rules="groupNameRules"
            autofocus
            required
          ></v-text-field>
        </v-card-item>
        <v-card-actions>
          <v-col class="text-right">
            <v-btn type="submit" color="primary">
              {{ t('groups.groupForm.save') }}
            </v-btn>
            <v-btn @click="emit('update:dialogOpen', false)">
              {{ t('groups.groupForm.cancel') }}
            </v-btn>
          </v-col>
        </v-card-actions>
      </v-card>
    </v-form>
  </v-dialog>
</template>

<style scoped lang="scss"></style>
