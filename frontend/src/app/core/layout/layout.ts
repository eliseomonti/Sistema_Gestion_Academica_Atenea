import { Component, effect, inject, signal } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';
import { rutaLogin } from '../seguridad/rol';
import { Sesion } from '../seguridad/sesion';
import { Menu } from './menu/menu';
import { MENUS } from './menus';

/**
 * La estructura común de las pantallas de los tres actores: barra superior, menú según el rol y la
 * pantalla de la ruta activa. Envuelve a /admin, /docente y /estudiante desde app.routes.ts.
 */
@Component({
  selector: 'app-layout',
  imports: [RouterOutlet, Menu],
  templateUrl: './layout.html',
  styleUrl: './layout.css',
  // Escucha los clics en toda la página, no sólo adentro del layout.
  host: { '(document:click)': 'cerrarMenuSiTocaAfuera($event)' },
})
export class Layout {
  private readonly sesion = inject(Sesion);
  private readonly router = inject(Router);

  /**
   * El rol con el que se entró. Se guarda al crear el layout porque, una vez cerrada la sesión,
   * `sesion.rol()` ya es null y hace falta saber a qué pantalla de login volver.
   */
  private readonly rol = this.sesion.rol() ?? 'ESTUDIANTE';

  protected readonly menu = MENUS[this.rol];

  /** Si el menú está desplegado. Sólo importa en celular y tablet: en escritorio se ve siempre. */
  protected readonly menuAbierto = signal(false);

  constructor() {
    // Si la sesión se cierra desde otra pestaña, o vence y no se puede renovar, vuelve al login.
    effect(() => {
      if (!this.sesion.accessToken()) {
        void this.router.navigate(rutaLogin(this.rol));
      }
    });
  }

  /** Cierra el menú si se toca cualquier lugar que no sea el menú ni el botón que lo abre. */
  protected cerrarMenuSiTocaAfuera(evento: MouseEvent): void {
    const tocado = evento.target as HTMLElement;
    if (this.menuAbierto() && !tocado.closest('#menu-principal, .hamburguesa')) {
      this.menuAbierto.set(false);
    }
  }

  protected salir(): void {
    this.sesion.cerrar();
    // No hace falta navegar acá: el effect de arriba ve que no hay sesión y vuelve al login.
  }
}
