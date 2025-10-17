import { NgxLoggerLevel } from 'ngx-logger';
export const environment = {
  production: true,
  serverUrl: 'https://smart-stocks-1c68.onrender.com',
  modelUrl: 'http://localhost:5000',
  logLevel: NgxLoggerLevel.INFO,
  serverLoggingUrl: '/api/logs',
  serverLogLevel: NgxLoggerLevel.OFF,
};
