import { Component, inject } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { DiaSemana } from '../../../../shared/models/dia-semana';
import { PeriodoLectivo } from '../../../../shared/models/periodo-lectivo';
import { RolDocente } from '../../../../shared/models/rol-docente';
import { Celda } from '../../../../shared/ui/tabla/celda';
import { Columna } from '../../../../shared/ui/tabla/columna';
import { Tabla } from '../../../../shared/ui/tabla/tabla';
import { HorarioResponse, MateriaDocenteResponse } from '../../data/comision-materia';
import { ComisionesMateriaApi } from '../../data/comisiones-materia-api';

const PERIODOS: Record<PeriodoLectivo, string> = {
  PRIMER_CUATRIMESTRE: '1.er cuatrimestre',
  SEGUNDO_CUATRIMESTRE: '2.º cuatrimestre',
  ANUAL: 'Anual',
};

const DIAS: Record<DiaSemana, string> = {
  LUNES: 'Lun',
  MARTES: 'Mar',
  MIERCOLES: 'Mié',
  JUEVES: 'Jue',
  VIERNES: 'Vie',
  SABADO: 'Sáb',
  DOMINGO: 'Dom',
};

const ROLES: Record<RolDocente, string> = {
  TITULAR: 'Titular',
  AYUDANTE: 'Ayudante',
};

/**
 * Las materias que dicta el docente logueado en el año lectivo actual, agrupadas por período. El
 * backend ya las devuelve ordenadas por período y comisión, que es lo que necesita la tabla para
 * agruparlas.
 */
@Component({
  selector: 'app-comisiones-materia-lista',
  imports: [Tabla, Celda],
  templateUrl: './comisiones-materia-lista.html',
  styleUrl: './comisiones-materia-lista.css',
})
export class ComisionesMateriaLista {
  private readonly api = inject(ComisionesMateriaApi);

  protected readonly materias = rxResource({ stream: () => this.api.listar() });

  protected readonly columnas: Columna<MateriaDocenteResponse>[] = [
    { clave: 'nombreMateria', titulo: 'Materia', principal: true },
    { clave: 'nombreComision', titulo: 'Comisión', tituloEnLista: true },
    { clave: 'horarios', titulo: 'Horarios' }, // lo muestra <ng-template appCelda>
    { clave: 'rol', titulo: 'Rol', valor: (materia) => ROLES[materia.rol] },
  ];

  /** "1.er cuatrimestre" */
  protected readonly grupo = (materia: MateriaDocenteResponse) => PERIODOS[materia.periodoLectivo];

  protected readonly cantidad = (n: number) => (n === 1 ? '1 materia' : `${n} materias`);

  protected readonly detalle = (materia: MateriaDocenteResponse) => [materia.comisionMateriaId];

  /** "Lun 08:00 a 10:00 · Aula 101". Sin aula, la cursada es virtual. */
  protected horario(horario: HorarioResponse): string {
    const horas = `${hhmm(horario.horaInicio)} a ${hhmm(horario.horaFin)}`;
    return `${DIAS[horario.diaSemana]} ${horas} · ${horario.aulaNombre ?? 'Virtual'}`;
  }
}

/** "08:30:00" → "08:30". */
function hhmm(hora: string): string {
  return hora.slice(0, 5);
}
