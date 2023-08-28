import './main.scss';
import '@mdi/font/css/materialdesignicons.css';
import 'vuetify/styles';
import { aliases, mdi } from 'vuetify/iconsets/mdi';
import { createApp } from 'vue';
import { createPinia } from 'pinia';
import { createVuetify } from 'vuetify';
import { i18n } from '@/i18n';
import App from './App.vue';
import axios from 'axios';
import piniaPluginPersistedState from 'pinia-plugin-persistedstate';
import router from './router';

if (import.meta.env.DEV) {
  axios.defaults.baseURL = 'http://localhost:3000';
}
axios.defaults.timeout = 5000;

const vuetify = createVuetify({
  theme: {
    themes: {
      light: {
        colors: {
          primary: '#0a3040',
          secondary: '#144c71',
          accent: '#1c65a1',
          error: '#ff5252',
          background: '#f5f5f5',
        },
      },
      dark: {
        colors: {
          primary: '#1c65a1',
          secondary: '#144c71',
          accent: '#0a3040',
          error: '#ff5252',
        },
      },
    },
  },
  icons: {
    defaultSet: 'mdi',
    aliases,
    sets: {
      mdi,
    },
  },
});

const store = createPinia();

store.use(piniaPluginPersistedState);

const app = createApp(App);

app.use(vuetify);
app.use(i18n);
app.use(router);
app.use(store);

app.mount('#app');
