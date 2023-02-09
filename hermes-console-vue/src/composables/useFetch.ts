import { isRef, ref, unref, watchEffect } from 'vue';

// https://vuejs.org/guide/reusability/composables.html#async-state-example
export function useFetch(url: string) {
  const data = ref(null);
  const error = ref(null);

  function doFetch() {
    // reset state before fetching..
    data.value = null;
    error.value = null;
    // unref() unwraps potential refs
    fetch(unref(url))
      .then((res) => res.json())
      .then((json) => (data.value = json))
      .catch((err) => (error.value = err));
  }

  if (isRef(url)) {
    // setup reactive re-fetch if input URL is a ref
    watchEffect(doFetch);
  } else {
    // otherwise, just fetch once
    // and avoid the overhead of a watcher
    doFetch();
  }

  return { data, error };
}
