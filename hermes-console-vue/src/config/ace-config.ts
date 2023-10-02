import ace from 'ace-builds';

import modeJsonUrl from 'ace-builds/src-noconflict/mode-json?url';
ace.config.setModuleUrl('ace/mode/json', modeJsonUrl);

import themeGithubUrl from 'ace-builds/src-noconflict/theme-github?url';
ace.config.setModuleUrl('ace/theme/github', themeGithubUrl);

import workerJsonUrl from 'ace-builds/src-noconflict/worker-json?url';
ace.config.setModuleUrl('ace/mode/json_worker', workerJsonUrl);
