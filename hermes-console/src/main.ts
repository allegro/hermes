import './main.scss';
import '@mdi/font/css/materialdesignicons.css';
import 'vuetify/styles';
import { aliases, mdi } from 'vuetify/iconsets/mdi';
import { createApp } from 'vue';
import { createPinia } from 'pinia';
import { createVuetify } from 'vuetify';
import { i18n } from '@/i18n';
import App from './App.vue';
import colors from 'vuetify/util/colors';
import piniaPluginPersistedState from 'pinia-plugin-persistedstate';
import router from './router';

const vuetify = createVuetify({
  theme: {
    themes: {
      light: {
        colors: {
          primary: '#3766a5',
          secondary: '#144c71',
          accent: '#1c65a1',
          background: '#f8fafc',
        },
      },
      dark: {
        colors: {
          primary: colors.blue.darken2,
          secondary: colors.blue.darken3,
          accent: colors.blue.accent1,
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
  defaults: {
    VCard: {
      border: true,
      flat: true,
      rounded: 'lg',
    },
    VBreadcrumbs: {
      density: 'compact',
    },
    VBtn: {
      variant: 'flat',
    },
    VRow: {
      dense: true,
    },
    VTextField: {
      bgColor: 'surface',
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
