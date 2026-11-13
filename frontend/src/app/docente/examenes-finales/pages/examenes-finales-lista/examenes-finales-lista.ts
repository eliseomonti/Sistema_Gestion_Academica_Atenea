import { Component, inject } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { Columna } from '../../../../shared/ui/tabla/columna';
import { Tabla } from '../../../../shared/ui/tabla/tabla';
import { MesaDocenteResponse } from '../../data/examen-final';
import { ExamenesFinalesApi } from '../../data/examenes-finales-api';

/**
 * Las mesas de examen del año actual en cuyo tribunal está el docente logueado. El backend ya las
 * devuelve ordenadas por fecha y hora.
 */
@Component({
  selector: 'app-examenes-finales-lista',
  imports: [Tabla],
  templateUrl: './examenes-finales-lista.html',
  styleUrl: './examenes-finales-lista.css',
})
export class ExamenesFinalesLista {
  private readonly api = inject(ExamenesFinalesApi);

  protected readonly mesas = rxResource({ stream: () => this.api.listar() });

  protected readonly columnas: Columna<MesaDocenteResponse>[] = [
    { clave: 'fecha', titulo: 'Fecha', valor: (m) => ddmmaaaa(m.fecha), tituloEnLista: true },
    { clave: 'nombreMateria', titulo: 'Materia', principal: true },
    {
      clave: 'horario',
      titulo: 'Horario',
      valor: (m) => `${hhmm(m.horaInicio)} a ${hhmm(m.horaFin)}`,
      tituloEnLista: true,
    },
    // Sin aula, la mesa es virtual.
    { clave: 'nombreAula', titulo: 'Aula', valor: (m) => m.nombreAula ?? 'Virtual' },
  ];

  /** Cada fila abre la planilla de notas de la mesa. */
  protected readonly enlace = (mesa: MesaDocenteResponse) => [mesa.examenFinalId];
}

/** "2026-05-14" → "14/05/2026". */
function ddmmaaaa(fecha: string): string {
  return fecha.split('-').reverse().join('/');
}

/** "08:30:00" → "08:30". */
function hhmm(hora: string): string {
  return hora.slice(0, 5);
}
