// This file can be replaced during build by using the `fileReplacements` array.
// `ng build` replaces `environment.ts` with `environment.prod.ts`.
// The list of file replacements can be found in `angular.json`.

export const environment = {
  production: false,
  serverUrl: 'http://localhost:8080',
  modelUrl: 'http://localhost:5000',
  // Massive Stock Market API — get your key at https://massive.com/dashboard/keys
  massiveApiUrl: 'https://api.massive.com',
  massiveApiKey: 'YGtR9Qi9j4axrqEqZ4pEGxp7F5N29ATb',
  googleClientId: '44421379309-pdeqjdtbcag0tr0cbt3hqlvr1qde0hk0.apps.googleusercontent.com'
};

/*
 * For easier debugging in development mode, you can import the following file
 * to ignore zone related error stack frames such as `zone.run`, `zoneDelegate.invokeTask`.
 *
 * This import should be commented out in production mode because it will have a negative impact
 * on performance if an error is thrown.
 */
// import 'zone.js/plugins/zone-error';  // Included with Angular CLI.
