import axios from 'axios';

const instance = axios.create({
  baseURL: import.meta.env.DEV
    ? 'http://localhost:3000'
    : window.location.origin,
  timeout: 1000 * 30, //30s
});

export default instance;
