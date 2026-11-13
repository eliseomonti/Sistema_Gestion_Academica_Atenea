import { Component, computed, input } from '@angular/core';
import { Turno } from '../../../../shared/models/turno';
import { BLOQUES } from '../../data/bloques';
import { DiaSemana } from '../../../../shared/models/dia-semana';
import { ComisionMateriaResponse, DocenteAsignadoResponse } from '../../data/comision';

/** El cuatrimestre que muestra la grilla. Las materias anuales aparecen en los dos. */
export type Cuatrimestre = 'PRIMER_CUATRIMESTRE' | 'SEGUNDO_CUATRIMESTRE';

/** Lo que muestra una celda: una clase de una materia, un día en una franja horaria. */
interface Clase {
  dia: DiaSemana;
  /** "08:30 a 10:30". También dice en qué fila va. */
  horas: string;
  materia: string;
  titular: string | null;
  ayudantes: string | null;
  aula: string;
}

const DIAS: { dia: DiaSemana; nombre: string }[] = [
  { dia: 'LUNES', nombre: 'Lunes' },
  { dia: 'MARTES', nombre: 'Martes' },
  { dia: 'MIERCOLES', nombre: 'Miércoles' },
  { dia: 'JUEVES', nombre: 'Jueves' },
  { dia: 'VIERNES', nombre: 'Viernes' },
  { dia: 'SABADO', nombre: 'Sábado' },
  { dia: 'DOMINGO', nombre: 'Domingo' },
];

/** El backend puede mandar la hora con segundos ("08:30:00"): se muestra sólo "08:30". */
function hhmm(hora: string): string {
  return hora.slice(0, 5);
}

/** "E. Etcheber", como en la planilla. */
function abreviado(docente: DocenteAsignadoResponse): string {
  return `${docente.nombre.charAt(0)}. ${docente.apellido}`;
}

/** Una clase por cada horario de la materia, con los mismos docentes en todas. */
function clasesDe(materia: ComisionMateriaResponse): Clase[] {
  const titular = materia.docentes.find((d) => d.rol === 'TITULAR');
  const ayudantes = materia.docentes.filter((d) => d.rol === 'AYUDANTE').map(abreviado);
  const textoAyudantes =
    ayudantes.length === 0
      ? null
      : `${ayudantes.length === 1 ? 'Ayudante' : 'Ayudantes'}: ${ayudantes.join(', ')}`;

  return materia.horarios.map((horario) => ({
    dia: horario.diaSemana,
    horas: `${hhmm(horario.horaInicio)} a ${hhmm(horario.horaFin)}`,
    materia: materia.nombreMateria,
    titular: titular ? abreviado(titular) : null,
    ayudantes: textoAyudantes,
    aula: horario.aulaNombre ?? 'Virtual',
  }));
}

/**
 * La semana de una comisión, como la planilla en papel: los días en las columnas y los bloques
 * horarios del turno en las filas.
 *
 * En el HTML las celdas van ordenadas por día: en celular se ven así, un día debajo del otro, y
 * desde 48rem el CSS ubica cada una en su columna y su fila (variables --columna y --fila).
 */
@Component({
  selector: 'app-grilla-horaria',
  templateUrl: './grilla-horaria.html',
  styleUrl: './grilla-horaria.css',
})
export class GrillaHoraria {
  readonly materias = input.required<ComisionMateriaResponse[]>();
  readonly cuatrimestre = input.required<Cuatrimestre>();
  /** El turno de la comisión: sus bloques son las filas de la grilla. */
  readonly turno = input.required<Turno>();

  protected readonly clases = computed(() =>
    this.materias()
      .filter((m) => m.periodoLectivo === 'ANUAL' || m.periodoLectivo === this.cuatrimestre())
      .flatMap(clasesDe),
  );

  /**
   * Las filas: los bloques del turno, siempre, aunque estén vacíos. Si algún horario no coincide
   * con ningún bloque (cargado por SQL, o de antes de que fueran fijos), va en una fila más, para
   * que no desaparezca. "08:30 a 10:30" ordenado como texto queda por hora.
   */
  protected readonly franjas = computed(() => {
    const bloques = BLOQUES[this.turno()].map((bloque) => `${bloque.inicio} a ${bloque.fin}`);
    const horarios = this.clases().map((clase) => clase.horas);
    return [...new Set([...bloques, ...horarios])].sort();
  });

  /**
   * Las columnas: de lunes a viernes siempre, y sábado o domingo sólo si tienen clases. Cada día
   * trae una celda por franja, con su clase o null si ese hueco está libre.
   */
  protected readonly dias = computed(() => {
    const franjas = this.franjas();
    return DIAS.map((dia) => {
      const delDia = this.clases().filter((clase) => clase.dia === dia.dia);
      return {
        ...dia,
        vacio: delDia.length === 0,
        celdas: franjas.map((franja) => delDia.find((clase) => clase.horas === franja) ?? null),
      };
    }).filter((dia, indice) => indice < 5 || !dia.vacio);
  });
}
