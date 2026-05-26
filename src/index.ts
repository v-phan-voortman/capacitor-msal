import { registerPlugin } from '@capacitor/core';

import type { MsalPlugin } from './definitions';

const Msal = registerPlugin<MsalPlugin>('Msal', {
  web: () => import('./web').then((m) => new m.MsalWeb()),
});

export * from './definitions';
export { Msal };
