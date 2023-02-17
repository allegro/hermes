import './main.scss';
import '@mdi/font/css/materialdesignicons.css';
import 'vuetify/styles';
import { aliases, mdi } from 'vuetify/iconsets/mdi';
import { createApp } from 'vue';
import { createVuetify } from 'vuetify';
import App from './App.vue';
import axios from 'axios';
import router from './router';

// TODO: should be fetched from Hermes console configuration
axios.defaults.baseURL = 'http://localhost:3000';
axios.defaults.timeout = 1500;

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

const app = createApp(App);

app.use(vuetify);
app.use(router);

app.mount('#app');
