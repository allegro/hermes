var config = {
    "console": {
        "title": "hermes console"
    },
    "dashboard": {
        "metrics": "",
        "docs": "http://hermes-pubsub.rtfd.org"
    },
    "hermes": {
        "url": "http://hermes.allegro.tech"
    },
    "metrics": {
        "type": "graphite",
        "graphite": {
            "url": "localhost:8091",
            "prefix": "hermes"
        }
    },
    "auth": {
        "oauth": {
            "enabled": false,
            "url": "localhost:8092/auth",
            "clientId": "hermes",
            "scope": "hermes"
        },
        "headers": {
            "enabled": false,
            "groupHeader": "Hermes-Group-Password",
            "adminHeader": "Hermes-Admin-Password"
        }
    }
};
