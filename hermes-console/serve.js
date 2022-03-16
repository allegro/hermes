#!/usr/bin/env node

var static = require('node-static');

var request = require('request');
var fs = require('fs');

var _ = require('lodash');

var DEFAULT_CONFIG = './config_default.json';

var yargs = require('yargs')
    .env('HERMES_CONSOLE')
    .option('p', {
        alias: 'port',
        default: 8000,
        demand: true
    })
    .option('c', {
        alias: 'config',
        default: './config.json',
        demand: true
    })
    .argv;

var port = yargs.port;

readConfiguration(yargs.config, startServer);

function startServer(config) {
    console.log('Starting Hermes Console at port: ' + port);
    console.log('Config: ' + JSON.stringify(config, null, 2));

    var file = new static.Server('./static');
    require('http').createServer((request, response) => {
        response.setHeader('Pragma', 'no-cache');
        if(request.url == '/console') {
            response.setHeader('Content-Type', 'application/json');
            response.end('var config = ' + JSON.stringify(config) + ';', 'UTF-8');
        }
        else if(request.url == '/status/ping') {
            response.end('pong', 'UTF-8');
        }
        else {
            request.addListener('end', function () {
                file.serve(request, response);
            }).resume();
        }
    }).listen(port);
}

function readConfiguration(source, callback) {
    console.log('Reading default configuration from file: ' + DEFAULT_CONFIG);
    var defaultConfig = JSON.parse(fs.readFileSync(DEFAULT_CONFIG, 'utf8'));

    if (source.indexOf('htt') === 0) {
        console.log('Reading configuration from remote source: ' + source);
        request.get(source, (error, res, body) => { callback(mergeConfig(defaultConfig, JSON.parse(body))); });
    }
    else {
        console.log('Reading configuration from file: ' + source);
        var config = JSON.parse(fs.readFileSync(source, 'utf8'));
        callback(mergeConfig(defaultConfig, config));
    }
}

function mergeConfig(defaultConfig, config) {
    return _.mergeWith(defaultConfig, config, customizer)
}

function customizer(objValue, srcValue) {
    if (_.isArray(objValue)) {
        return srcValue
    }
}