<script setup lang="ts">
  import { computed, ComputedRef, Ref, ref } from 'vue';
  import { v4 as generateUUID } from 'uuid';
  import { PathFilter } from '@/views/subscription/subscription-form/subscription-basic-filters/types';
  import { useI18n } from 'vue-i18n';
  import { useSubscriptionFiltersDebug } from '@/composables/subscription/use-subscription-filters-debug/useSubscriptionFiltersDebug';
  import { VerificationStatus } from '@/api/message-filters-verification';
  import ConsoleAlert from '@/components/console-alert/ConsoleAlert.vue';
  import SubscriptionPathFilters from '@/views/subscription/subscription-form/subscription-basic-filters/SubscriptionPathFilters.vue';

  const dialogVisible = ref(false);

  const props = defineProps<{
    topic: string;
    modelValue: PathFilter[];
  }>();
  const { t } = useI18n();

  const copy = (pathFilter: PathFilter): PathFilter => {
    return {
      id: generateUUID(),
      path: pathFilter.path,
      matcher: pathFilter.matcher,
      matchingStrategy: pathFilter.matchingStrategy,
    };
  };

  const debugFilters: Ref<PathFilter[]> = ref(
    props.modelValue.map((v) => copy(v)),
  );

  const { status, errorMessage, verify } = useSubscriptionFiltersDebug();

  const message = ref('');

  const emit = defineEmits(['update:modelValue']);

  const onDebugOpen = () => {
    dialogVisible.value = true;
  };

  const onCancel = () => {
    dialogVisible.value = false;
  };

  const onSave = () => {
    emit('update:modelValue', debugFilters.value);
    dialogVisible.value = false;
  };

  const onVerify = () => {
    verify(props.topic, debugFilters.value, message.value);
  };

  interface VerificationAlert {
    title: string;
    type: 'success' | 'warning' | 'error';
    text: string;
  }

  const alert: ComputedRef<VerificationAlert | undefined> = computed(() => {
    if (status.value == undefined) {
      return undefined;
    }
    switch (status.value!!) {
      case VerificationStatus.MATCHED:
        return {
          title: t('filterDebug.matched'),
          text: '',
          type: 'success',
        };
      case VerificationStatus.NOT_MATCHED:
        return {
          title: t('filterDebug.notMatched'),
          text: '',
          type: 'warning',
        };
      case VerificationStatus.ERROR:
        return {
          title: t('filterDebug.error'),
          text: errorMessage.value!!,
          type: 'error',
        };
      default:
        return undefined;
    }
  });
</script>
<template>
  <v-col class="md-4 text-right">
    <v-btn color="green" @click="onDebugOpen">
      {{ $t('filterDebug.debugButton') }}</v-btn
    >
    <v-dialog width="100%" min-width="50%" v-model="dialogVisible">
      <v-card>
        <v-card-title>{{ $t('filterDebug.title') }}</v-card-title>
        <v-card-item>
          <v-textarea
            :placeholder="$t('filterDebug.placeholder')"
            v-model="message"
          ></v-textarea>
        </v-card-item>
        <v-card-item class="text-right">
          <v-btn color="green" @click="onVerify">
            {{ $t('filterDebug.verifyButton') }}</v-btn
          >
        </v-card-item>
        <v-card-item v-if="alert">
          <console-alert
            :title="alert.title"
            :text="alert.text"
            :type="alert.type"
          ></console-alert>
        </v-card-item>
        <v-card-item class="mt-4">
          <subscription-path-filters v-model="debugFilters" />
        </v-card-item>
        <v-card-item class="text-right mb-4">
          <v-btn color="primary" class="mr-4" @click="onSave">
            {{ $t('filterDebug.saveButton') }}
          </v-btn>
          <v-btn color="orange" @click="onCancel">{{
            $t('filterDebug.cancelButton')
          }}</v-btn>
        </v-card-item>
      </v-card>
    </v-dialog>
  </v-col>
</template>
