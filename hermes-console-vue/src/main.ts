import '@mdi/font/css/materialdesignicons.css';
import 'vuetify/styles';
import { aliases, mdi } from 'vuetify/iconsets/mdi';
import { createApp } from 'vue';
import { createVuetify } from 'vuetify';
import App from './App.vue';
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

const app = createApp(App);

app.use(vuetify);
app.use(router);

app.mount('#app');
