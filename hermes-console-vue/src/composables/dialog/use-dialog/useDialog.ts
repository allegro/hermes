import { ref } from 'vue';
import type { Ref } from 'vue';

export interface UseDialog {
  isDialogOpened: Ref<boolean>;
  actionButtonEnabled: Ref<boolean>;
  openDialog: () => Promise<void>;
  closeDialog: () => Promise<void>;
  enableActionButton: () => Promise<void>;
  disableActionButton: () => Promise<void>;
}

export function useDialog(
  isOpenedByDefault: boolean = false,
  actionButtonEnabledByDefault: boolean = true,
): UseDialog {
  const isDialogOpened = ref(isOpenedByDefault);
  const actionButtonEnabled = ref(actionButtonEnabledByDefault);

  async function openDialog() {
    isDialogOpened.value = true;
  }

  async function closeDialog() {
    isDialogOpened.value = false;
  }

  async function enableActionButton() {
    actionButtonEnabled.value = true;
  }

  async function disableActionButton() {
    actionButtonEnabled.value = false;
  }

  return {
    isDialogOpened,
    actionButtonEnabled,
    openDialog,
    closeDialog,
    enableActionButton,
    disableActionButton,
  };
}
