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

const router = jsonServer.router('json-server/db.json');
server.use(router);

server.listen(3000, () => {
  console.log('JSON Server is running');
});
