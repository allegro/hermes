<script setup lang="ts">
  import { ref } from 'vue';
  import { useDialog } from '@/composables/dialog/use-dialog/useDialog';
  import { useI18n } from 'vue-i18n';
  import { useReadiness } from '@/composables/readiness/use-readiness/useReadiness';
  import { useRouter } from 'vue-router';
  import ConfirmationDialog from '@/components/confirmation-dialog/ConfirmationDialog.vue';
  import ConsoleAlert from '@/components/console-alert/ConsoleAlert.vue';
  import LoadingSpinner from '@/components/loading-spinner/LoadingSpinner.vue';

  const { t } = useI18n();

  const router = useRouter();

  const { datacentersReadiness, loading, error, switchReadinessState } =
    useReadiness();

  const breadcrumbsItems = [
    {
      title: t('readiness.breadcrumbs.home'),
      href: '/',
    },
    {
      title: t('readiness.breadcrumbs.title'),
    },
  ];

  const dcToSwitch = ref();
  const readinessState = ref();

  const {
    isDialogOpened: isSwitchDialogOpened,
    actionButtonEnabled: actionSwitchButtonEnabled,
    openDialog: openSwitchDialog,
    closeDialog: closeSwitchDialog,
    enableActionButton: enableSwitchActionButton,
    disableActionButton: disableSwitchActionButton,
  } = useDialog();

  async function switchReadiness() {
    disableSwitchActionButton();
    const isReadinessChanged = await switchReadinessState(
      dcToSwitch.value,
      !readinessState.value,
    );
    enableSwitchActionButton();
    closeSwitchDialog();
    if (isReadinessChanged) {
      router.go(0);
    }
  }

  function openSwitchReadinessDialog(datacenter: string, isReady: boolean) {
    dcToSwitch.value = datacenter;
    readinessState.value = isReady;
    openSwitchDialog();
  }
</script>

<template>
  <confirmation-dialog
    v-model="isSwitchDialogOpened"
    :actionButtonEnabled="actionSwitchButtonEnabled"
    :title="$t('readiness.confirmationDialog.switch.title')"
    :text="
      t('readiness.confirmationDialog.switch.text', {
        dcToSwitch: dcToSwitch,
        switchAction: readinessState
          ? t('readiness.turnOff')
          : t('readiness.turnOn'),
      })
    "
    @action="switchReadiness"
    @cancel="closeSwitchDialog"
  />
  <v-container fill-height fluid class="mx-auto">
    <v-row dense>
      <v-col md="12">
        <v-breadcrumbs :items="breadcrumbsItems" density="compact" />
        <loading-spinner v-if="loading" />
        <console-alert
          v-if="error.fetchReadiness"
          :title="$t('readiness.connectionError.title')"
          :text="$t('readiness.connectionError.text')"
          type="error"
        />
      </v-col>
    </v-row>
    <v-container v-if="!loading && !error.fetchReadiness">
      <v-row dense>
        <v-col md="12">
          <p class="text-h4 font-weight-bold mb-3">
            {{ $t('readiness.title') }}
          </p>
        </v-col>
      </v-row>
      <v-row dense>
        <v-col md="12">
          <v-table>
            <thead>
              <tr>
                <th class="text-left">{{ $t('readiness.index') }}</th>
                <th class="text-left">{{ $t('readiness.datacenter') }}</th>
                <th class="text-left">{{ $t('readiness.status') }}</th>
                <th class="text-left">{{ $t('readiness.control') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(item, index) in datacentersReadiness" :key="index">
                <td>{{ index + 1 }}</td>
                <td>{{ item.datacenter }}</td>
                <td>{{ item.status }}</td>
                <td class="w-0">
                  <v-btn
                    variant="text"
                    v-if="item.status == 'NOT_READY'"
                    prepend-icon="mdi-console-line"
                    color="green"
                    block
                    @click="openSwitchReadinessDialog(item.datacenter, false)"
                  >
                    {{ $t('readiness.turnOn') }}</v-btn
                  >
                  <v-btn
                    variant="text"
                    v-if="item.status == 'READY'"
                    prepend-icon="mdi-console-line"
                    color="red"
                    block
                    @click="openSwitchReadinessDialog(item.datacenter, true)"
                  >
                    {{ $t('readiness.turnOff') }}</v-btn
                  >
                </td>
              </tr>
            </tbody>
          </v-table>
        </v-col>
      </v-row>
    </v-container>
  </v-container>
</template>

<style lang="scss"></style>
