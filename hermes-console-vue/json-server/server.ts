const topics = require('./topics.json');
const subscriptions = require('./subscriptions.json');
const routes = require('./routes.json');

const jsonServer = require('json-server');
const server = jsonServer.create();

const middlewares = jsonServer.defaults();
server.use(middlewares);

const rewriter = jsonServer.rewriter(routes);
server.use(rewriter);

server.post('/query/topics', (req, res) => {
  res.jsonp(topics);
});

server.post('/query/subscriptions', (req, res) => {
  res.jsonp(subscriptions);
});

server.post('/topics/*/subscriptions/*/moveOffsetsToTheEnd', (req, res) => {
  res.sendStatus(200);
});

server.delete('/groups/:group', (req, res) => {
  res.sendStatus(200);
});

server.delete('/topics/:topic', (req, res) => {
  res.sendStatus(200);
});

server.delete('/subscriptions/:subscription', (req, res) => {
  res.sendStatus(200);
});

server.put('/topics/*/subscriptions/*/state', (req, res) => {
  res.sendStatus(200);
});

server.delete('/consistency/inconsistencies/topics', (req, res) => {
  res.sendStatus(200);
});

server.post('/readiness/datacenters/:dc', (req, res) => {
  res.sendStatus(200);
});

server.put('/workload-constraints/*', (req, res) => {
  res.sendStatus(200);
});

server.delete('/workload-constraints/*', (req, res) => {
  res.sendStatus(200);
});

server.post('/groups', (req, res) => {
  res.sendStatus(200);
});

server.post('/offline-retransmission/tasks', (req, res) => {
  res.sendStatus(200);
});

const router = jsonServer.router('json-server/db.json');
server.use(router);

server.listen(3000, () => {
  console.log('JSON Server is running');
});
