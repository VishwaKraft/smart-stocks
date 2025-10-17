import { NgxLoggerLevel } from 'ngx-logger';

export const environment = {
  production: false,
  serverUrl: 'http://localhost:8080',
  modelUrl: 'http://localhost:5000',
  logLevel: NgxLoggerLevel.TRACE,
  serverLoggingUrl: '/api/logs',
  serverLogLevel: NgxLoggerLevel.OFF,
};
