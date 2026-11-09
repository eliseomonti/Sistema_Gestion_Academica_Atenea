import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import {
  provideRouter,
  TitleStrategy,
  withComponentInputBinding,
  withRouterConfig,
} from '@angular/router';
import { routes } from './app.routes';
import { Titulo } from './core/layout/titulo';
import { authInterceptor } from './core/seguridad/auth-interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(
      routes,
      // withComponentInputBinding: los datos de la ruta llegan a la pantalla como input()
      withComponentInputBinding(),
      // 'always': una ruta hija también recibe los :parámetros de sus rutas padre. Lo necesitan las
      // pestañas del plan (materias) para recibir el :carreraId y el :planId de la pantalla del plan.
      withRouterConfig({ paramsInheritanceStrategy: 'always' }),
    ),
    provideHttpClient(withInterceptors([authInterceptor])),
    // El título de la pestaña: el `title` de cada ruta más " · Atenea"
    { provide: TitleStrategy, useClass: Titulo },
  ],
};
