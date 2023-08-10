// server.js
const topics = require('./topics.json');
const subscriptions = require('./subscriptions.json');

const jsonServer = require('json-server');
const server = jsonServer.create();
const router = jsonServer.router('db.json');
const middlewares = jsonServer.defaults();

server.use(middlewares);

server.post('/query/topics', (req, res) => {
  res.jsonp(topics);
});

server.post('/query/subscriptions', (req, res) => {
  res.jsonp(subscriptions);
});

server.use(router);
server.listen(3000, () => {
  console.log('JSON Server is running');
});
