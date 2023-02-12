import { isRef, ref, unref, watchEffect } from "vue";

export function useFetch<T>(url: string) {
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
      .catch(err => {
        console.log('ERROR!')
        console.log(err)
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
