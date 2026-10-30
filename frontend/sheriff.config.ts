import { sameTag, SheriffConfig } from '@softarc/sheriff-core';

/**
 * Reglas de dependencia del ADR 0002 (docs/adr/0002-estructura-frontend.md).
 */
export const config: SheriffConfig = {
  // Sin index.ts: todos los archivos de un módulo son públicos, salvo los de una carpeta internal/.
  enableBarrelLess: true,
  entryFile: 'src/main.ts',

  // Qué carpetas son módulos y qué etiquetas tienen. <feature> es cualquier nombre de carpeta.
  modules: {
    'src/app': {
      core: 'core',
      shared: 'shared',
      login: 'login',
      'admin/<feature>/data': ['actor:admin', 'tipo:data'],
      'admin/<feature>': ['actor:admin', 'tipo:feature'],
      'docente/<feature>/data': ['actor:docente', 'tipo:data'],
      'docente/<feature>': ['actor:docente', 'tipo:feature'],
      'estudiante/<feature>/data': ['actor:estudiante', 'tipo:data'],
      'estudiante/<feature>': ['actor:estudiante', 'tipo:feature'],
    },
  },

  // Qué puede importar cada etiqueta. Un import es válido sólo si TODAS las etiquetas
  // del módulo que importa lo permiten.
  depRules: {
    root: '*', // app.routes.ts y los xxx.routes.ts de cada actor cargan todo
    core: 'shared',
    shared: [], // shared no usa nada del proyecto
    login: ['core', 'shared'],
    'actor:*': [sameTag, 'core', 'shared'], // regla 1: un actor no importa a otro
    'tipo:feature': ['tipo:data', 'core', 'shared'], // regla 3: de otra feature, sólo su data/
    'tipo:data': ['tipo:data', 'core', 'shared'],
  },
};
