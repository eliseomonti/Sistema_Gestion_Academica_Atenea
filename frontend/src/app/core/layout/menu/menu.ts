import { Component, input, output } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { ItemMenu } from '../menus';

/** Lista de opciones del menú. Qué opciones mostrar lo decide el layout según el rol. */
@Component({
  selector: 'app-menu',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './menu.html',
  styleUrl: './menu.css',
})
export class Menu {
  readonly items = input.required<ItemMenu[]>();

  /** Avisa que se eligió una opción, para que el layout cierre el menú en el celular. */
  readonly elegido = output();
}
