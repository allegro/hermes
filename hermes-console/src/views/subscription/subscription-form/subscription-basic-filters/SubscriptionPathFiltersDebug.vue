<script setup lang="ts">
  import { computed, ref } from 'vue';
  import { v4 as generateUUID } from 'uuid';
  import { useGlobalI18n } from '@/i18n';
  import { useNotificationsStore } from '@/store/app-notifications/useAppNotifications';
  import { useSubscriptionFiltersDebug } from '@/composables/subscription/use-subscription-filters-debug/useSubscriptionFiltersDebug';
  import { VerificationStatus } from '@/api/message-filters-verification';
  import AceEditor from '@/components/ace-editor/AceEditor.vue';
  import ConsoleAlert from '@/components/console-alert/ConsoleAlert.vue';
  import SubscriptionPathFilters from '@/views/subscription/subscription-form/subscription-basic-filters/SubscriptionPathFilters.vue';
  import type { ComputedRef, Ref } from 'vue';
  import type { ContentType } from '@/api/content-type';
  import type { PathFilter } from '@/views/subscription/subscription-form/subscription-basic-filters/types';

  const props = defineProps<{
    topic: string;
    paths: string[];
    editEnabled: boolean;
    modelValue: PathFilter[];
  }>();
  const notificationStore = useNotificationsStore();
  const { t } = useGlobalI18n();

  const dialogVisible = ref(false);
  const topicContentType: Ref<ContentType | undefined> = ref();

  const copy = (pathFilter: PathFilter): PathFilter => {
    return {
      id: generateUUID(),
      path: pathFilter.path,
      matcher: pathFilter.matcher,
      matchingStrategy: pathFilter.matchingStrategy,
    };
  };

  const debugFilters: Ref<PathFilter[]> = ref([]);

  const { status, errorMessage, verify, fetchContentType } =
    useSubscriptionFiltersDebug();

  const message = ref('');

  const emit = defineEmits(['update:modelValue']);

  const onDebugOpen = async () => {
    debugFilters.value = props.modelValue.map((v) => copy(v));
    const contentType = await fetchContentType(props.topic);
    if (contentType.error != null) {
      notificationStore.dispatchNotification({
        title: t(
          'notifications.subscriptionFiltersDebug.fetchTopicContentType.failure',
          { topicName: props.topic },
        ),
        text: '',
        type: 'error',
      });
    } else {
      topicContentType.value = contentType.contentType!!;
      dialogVisible.value = true;
    }
  };

  const onCancel = () => {
    dialogVisible.value = false;
  };

  const onSave = () => {
    emit('update:modelValue', debugFilters.value);
    dialogVisible.value = false;
  };

  const onVerify = () => {
    verify(
      props.topic,
      debugFilters.value,
      message.value,
      topicContentType.value!!,
    );
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
    <v-btn
      color="green"
      @click="onDebugOpen"
      prepend-icon="mdi-console"
      data-testid="openFilterDebugButton"
    >
      {{ $t('filterDebug.debugButton') }}</v-btn
    >
    <v-dialog width="100%" min-width="50%" v-model="dialogVisible">
      <v-card>
        <v-card-title>{{ $t('filterDebug.title') }}</v-card-title>
        <v-card-item>
          <AceEditor
            v-model:model-value="message"
            :placeholder="$t('filterDebug.placeholder')"
          />
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
        <v-card-item>
          <subscription-path-filters v-model="debugFilters" :paths="paths" />
        </v-card-item>
        <v-card-item class="text-right mb-4">
          <v-btn
            color="primary"
            class="mr-4"
            @click="onSave"
            v-if="editEnabled"
          >
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
