import '@mdi/font/css/materialdesignicons.css';
import 'vuetify/styles';
import { aliases, mdi } from 'vuetify/iconsets/mdi';
import { createApp } from 'vue';
import { createI18n } from 'vue-i18n';
import { createVuetify } from 'vuetify';
import App from './App.vue';
import messages from './i18n/messages';
import router from './router';

const vuetify = createVuetify({
  theme: {
    themes: {
      light: {
        colors: {
          primary: '#0a3040',
          secondary: '#144c71',
          accent: '#1c65a1',
          error: '#ff5252',
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

const i18n = createI18n({
  legacy: false,
  locale: 'en-US',
  fallbackLocale: 'en-US',
  messages: messages,
});

const app = createApp(App);

app.use(vuetify);
app.use(i18n);
app.use(router);

app.mount('#app');
