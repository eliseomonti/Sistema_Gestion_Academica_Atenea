import { Component, computed, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { rxResource } from '@angular/core/rxjs-interop';
import { PeriodoLectivo } from '../../../inscripciones-comision/data/inscripcion-comision';
import { NotasApi } from '../../data/notas-api';
import { CursadaEstudianteResponse, NotaEvaluacionEstudianteResponse } from '../../data/notas';
import { Etiqueta } from '../../../../shared/ui/etiqueta/etiqueta';
import { Celda } from '../../../../shared/ui/tabla/celda';
import { Columna } from '../../../../shared/ui/tabla/columna';
import { Tabla } from '../../../../shared/ui/tabla/tabla';

/** Una fila de la tabla: las evaluaciones de una materia y, al final, su cierre. */
interface FilaCursada {
  grupo: string;
  nombre: string;
  fecha: string;
  nota: string;
  esCierre: boolean;
  promocion: boolean;
  cursada: CursadaEstudianteResponse;
}

/** "2026-04-20" → "20/04/2026" */
function fecha(iso: string | null | undefined): string {
  return iso ? iso.slice(0, 10).split('-').reverse().join('/') : '';
}

const CUATRIMESTRES: Record<PeriodoLectivo, string> = {
  PRIMER_CUATRIMESTRE: '1.er cuatrimestre',
  SEGUNDO_CUATRIMESTRE: '2.º cuatrimestre',
  ANUAL: 'Anual',
};

/**
 * Las notas de las cursadas en una tabla: cada materia es un grupo con sus evaluaciones y una
 * última fila con el cierre (nota y condición). Es la pestaña Cursadas de Mis notas.
 */
@Component({
  imports: [DatePipe, Etiqueta, Tabla, Celda],
  selector: 'app-notas-cursadas',
  styleUrl: './notas-cursadas.css',
  templateUrl: './notas-cursadas.html',
})
export class NotasCursadas {

  private readonly api = inject(NotasApi)
  protected readonly cursadas = rxResource({ stream: () => this.api.listarCursada() });

  protected readonly columnas: Columna<FilaCursada>[] = [
    { clave: 'nombre', titulo: 'Evaluación', principal: true },
    { clave: 'fecha', titulo: 'Fecha' },
    { clave: 'nota', titulo: 'Nota', tituloEnLista: true }, // <ng-template appCelda>
    { clave: 'estado', titulo: 'Estado' }, // <ng-template appCelda>
  ];

  protected readonly grupo = (fila: FilaCursada) => fila.grupo;

  protected readonly filas = computed<FilaCursada[]>(() =>
    (this.cursadas.value() ?? []).flatMap((cursada) => {
      const grupo = `${cursada.nombreMateria} · ${this.detalleCursada(cursada)}`;
      const evaluaciones = cursada.evaluaciones.map((evaluacion) => ({
        grupo,
        nombre: evaluacion.nombre,
        fecha: fecha(evaluacion.fecha),
        nota: this.notaExamen(evaluacion),
        esCierre: false,
        promocion: false,
        cursada,
      }));
      const cierre = {
        grupo,
        nombre: 'Cierre de cursada',
        fecha: fecha(cursada.cierre?.fechaCierre),
        nota: cursada.cierre?.nota != null ? String(cursada.cierre.nota) : '—',
        esCierre: true,
        promocion: cursada.cierre?.condicion === 'PROMOCIONADO',
        cursada,
      };
      return [...evaluaciones, cierre];
    }),
  );

  protected detalleCursada(cursada: CursadaEstudianteResponse): string
  {
    return `Comisión ${cursada.nombreComision} · ${CUATRIMESTRES[cursada.periodoLectivo]} ${cursada.anioLectivo}`
  }

  protected notaExamen(examen: NotaEvaluacionEstudianteResponse): string
  {
    if(examen.condicion === 'AUSENTE')
    {
      return 'Ausente';
    }

    return examen.nota !== null ? String(examen.nota) : '—';
  }

  /** Lo positivo va en una etiqueta azul; lo negativo, en texto gris, sin rojo. */
  protected condicionCursada(cursada: CursadaEstudianteResponse): {texto: string; tipo: 'info' | null} | null
  {
    switch(cursada.cierre?.condicion)
    {
      case 'PROMOCIONADO':
        return {texto: 'Promocionado', tipo: 'info'};
      case 'REGULAR':
        return {texto: 'Regular', tipo: 'info'};
      case 'NO_REGULAR':
        return {texto: 'Libre', tipo: null};
      default:
        return null;
    }
  }
}
