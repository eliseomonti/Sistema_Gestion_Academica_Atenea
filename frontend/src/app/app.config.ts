import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter, TitleStrategy, withComponentInputBinding } from '@angular/router';
import { routes } from './app.routes';
import { Titulo } from './core/layout/titulo';
import { authInterceptor } from './core/seguridad/auth-interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    // withComponentInputBinding: los datos de la ruta llegan a la pantalla como input()
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withInterceptors([authInterceptor])),
    // El título de la pestaña: el `title` de cada ruta más " · Atenea"
    { provide: TitleStrategy, useClass: Titulo },
  ],
};
