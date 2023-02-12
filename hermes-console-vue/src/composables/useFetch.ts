import { isRef, ref, unref, watchEffect } from "vue";

export function useFetch<T>(url: string, onSuccess?: (data: T) => void) {
  const data = ref<T | null>(null);
  const error = ref<any | null>(null);

  function doFetch() {
    data.value = null;
    error.value = null;
    fetch(unref(url))
      .then(res => {
        if (!res.ok) {
          throw new Error(`Request failed with status: ${res.status}`);
        }
        return res
      })
      .then(res => res.json())
      .then(json => (data.value = json))
      .then(json => {
        if (onSuccess) {
          onSuccess(json);
        }
        return json;
      })
      .catch(err => {
        error.value = err
      });
  }

  if (isRef(url)) {
    watchEffect(doFetch);
  } else {
    doFetch();
  }

  return { data, error };
}

export async function useAsyncFetch<T>(url: string) {
  const data = ref<T | null>(null);
  const error = ref<any | null>(null);

  async function doFetch() {
    data.value = null;
    error.value = null;
    try {
      const response = await fetch(unref(url));
      if (!response.ok) {
        error.value = Error(`Request failed with status: ${response.status}`);
        return;
      }
      data.value = await response.json();
    } catch(e) {
      error.value = e;
    }
  }

  if (isRef(url)) {
    watchEffect(doFetch);
  } else {
    await doFetch();
  }

  return { data, error };
}
