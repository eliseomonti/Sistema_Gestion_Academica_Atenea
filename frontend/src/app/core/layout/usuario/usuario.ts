import { Component, input, output } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { Icono } from '../../../shared/ui/icono/icono';

/** Al pie del menú: quién está logueado, que lleva a su cuenta, y el botón para salir. */
@Component({
  selector: 'app-usuario',
  imports: [RouterLink, RouterLinkActive, Icono],
  templateUrl: './usuario.html',
  styleUrl: './usuario.css',
})
export class Usuario {
  readonly nombre = input.required<string>();

  /** Avisa que se tocó el nombre, para que el layout cierre el menú en el celular. */
  readonly elegido = output();
  readonly salir = output();
}
