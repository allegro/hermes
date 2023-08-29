import { ref } from 'vue';
import type { Ref } from 'vue';

export interface UseDialog {
  isDialogOpened: Ref<boolean>;
  actionButtonEnabled: Ref<boolean>;
  openDialog: () => void;
  closeDialog: () => void;
  enableActionButton: () => void;
  disableActionButton: () => void;
}

export function useDialog(
  isOpenedByDefault: boolean = false,
  actionButtonEnabledByDefault: boolean = true,
): UseDialog {
  const isDialogOpened = ref(isOpenedByDefault);
  const actionButtonEnabled = ref(actionButtonEnabledByDefault);

  function openDialog() {
    isDialogOpened.value = true;
  }

  function closeDialog() {
    isDialogOpened.value = false;
  }

  function enableActionButton() {
    actionButtonEnabled.value = true;
  }

  function disableActionButton() {
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
