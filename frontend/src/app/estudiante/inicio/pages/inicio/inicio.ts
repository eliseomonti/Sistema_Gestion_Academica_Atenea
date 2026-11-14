import { Component, computed, inject } from '@angular/core';
import { Icono, NombreIcono } from '../../../../shared/ui/icono/icono';
import { Sesion } from '../../../../core/seguridad/sesion';
import { InscripcionesComisionApi } from '../../../inscripciones-comision/data/inscripciones-comision-api';
import { rxResource } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { Bienvenida } from '../../../../shared/ui/bienvenida/bienvenida';


interface Tarjeta {
  texto: string;
  ruta: string;
  icono: NombreIcono;
  total: string;
  detalle: string;
}

@Component({
  imports: [RouterLink, Bienvenida, Icono],
  selector: 'app-inicio',
  styleUrl: './inicio.css',
  templateUrl: './inicio.html',
})
export class Inicio {

  private readonly inscripcionesApi = inject(InscripcionesComisionApi);
  protected readonly sesion = inject(Sesion);

   private readonly oferta = rxResource({
    stream: () => this.inscripcionesApi.disponibilidad(),
  });

  private totalOferta(): string
  {
    if (this.oferta.hasValue()) {
      return String(this.oferta.value().length);
    }
    return this.oferta.error() ? '—' : '…';
  }

  protected readonly tarjetas = computed<Tarjeta[]>(() => [
    {
      texto: 'Inscripción a cursado',
      ruta: '/estudiante/inscripciones-comision',
      icono: 'editar',
      total: this.totalOferta(),
      detalle: 'materias para inscribirte',
    },
  ]);

  

}
