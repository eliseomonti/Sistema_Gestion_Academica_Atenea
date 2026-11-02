import { inject, Service } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { RouterStateSnapshot, TitleStrategy } from '@angular/router';

/**
 * Arma el título de la pestaña del navegador: el `title` de la ruta activa más " · Atenea".
 * Las rutas sin `title` muestran sólo "Atenea".
 */
@Service()
export class Titulo extends TitleStrategy {
  private readonly title = inject(Title);

  override updateTitle(snapshot: RouterStateSnapshot): void {
    const titulo = this.buildTitle(snapshot);
    this.title.setTitle(titulo ? `${titulo} · Atenea` : 'Atenea');
  }
}
