import { Component, computed, input } from '@angular/core';
import { Icono } from '../icono/icono';

/**
 * El cartel del inicio de cada actor: fecha, saludo según la hora y ciclo lectivo. El nombre llega
 * por input porque shared no puede leer la sesión:
 *
 * <app-bienvenida [nombre]="sesion.nombre()" />
 */
@Component({
  selector: 'app-bienvenida',
  imports: [Icono],
  templateUrl: './bienvenida.html',
  styleUrl: './bienvenida.css',
})
export class Bienvenida {
  readonly nombre = input<string | null>(null);

  private readonly hoy = new Date();
  protected readonly fecha = formatearFecha(this.hoy);
  protected readonly ciclo = `Ciclo lectivo ${this.hoy.getFullYear()} · ${this.hoy.getMonth() < 7 ? '1' : '2'}.º cuatrimestre`;
  protected readonly saludo = computed(() => {
    const nombre = this.nombre();
    return nombre ? `${saludoSegunHora(this.hoy)}, ${nombre}` : saludoSegunHora(this.hoy);
  });
}

/** 'Jueves 12 de noviembre'. */
function formatearFecha(fecha: Date): string {
  const dia = fecha.toLocaleDateString('es-AR', { weekday: 'long' });
  const mes = fecha.toLocaleDateString('es-AR', { month: 'long' });
  return `${dia[0].toUpperCase()}${dia.slice(1)} ${fecha.getDate()} de ${mes}`;
}

function saludoSegunHora(fecha: Date): string {
  const hora = fecha.getHours();
  if (hora >= 6 && hora < 12) {
    return 'Buenos días';
  }
  return hora >= 12 && hora < 20 ? 'Buenas tardes' : 'Buenas noches';
}
