import { Component, computed, inject, ResourceRef } from '@angular/core';
import { Sesion } from '../../../../core/seguridad/sesion';
import { Bienvenida } from '../../../../shared/ui/bienvenida/bienvenida';
import { Icono, NombreIcono } from '../../../../shared/ui/icono/icono';
import { DiaSemana } from '../../../../shared/models/dia-semana';
import { ComisionesMateriaApi } from '../../../comisiones-materia/data/comisiones-materia-api';
import { ExamenesFinalesApi } from '../../../examenes-finales/data/examenes-finales-api';
import { PeriodoLectivo } from '../../../../shared/models/periodo-lectivo';
import { DatePipe, formatDate } from '@angular/common';
import { rxResource } from '@angular/core/rxjs-interop';
import { MesaDocenteResponse } from '../../../examenes-finales/data/examen-final';
import { RouterLink } from '@angular/router';

const DIAS: DiaSemana[] = ['DOMINGO', 'LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES', 'SABADO'];

interface Tarjeta {
  texto: string;
  ruta: string | null;
  icono: NombreIcono;
  total: string;
  detalle: string;
}

interface Clase {
  comisionMateriaId: number;
  materia: string;
  comision: string;
  horaInicio: string;
  horaFin: string;
  aula: string;
}

/** La pantalla a la que se entra después del login. Por ahora, sólo la bienvenida. */
@Component({
  selector: 'app-inicio',
  imports: [Bienvenida, Icono, RouterLink, DatePipe],
  styleUrl: './inicio.css',
  templateUrl: './inicio.html',
})
export class Inicio {

  protected readonly sesion = inject(Sesion);

  private readonly comisionesApi = inject(ComisionesMateriaApi)
  private readonly finalesApi = inject(ExamenesFinalesApi);

  private readonly hoy = new Date()
  private readonly cuatrimestre: PeriodoLectivo = this.hoy.getMonth() < 7 ? 'PRIMER_CUATRIMESTRE' : 'SEGUNDO_CUATRIMESTRE';
  private readonly fechaHoy = formatDate(this.hoy, 'yyyy-MM-dd', 'en')

  protected readonly materias = rxResource({ stream: () => this.comisionesApi.listar() });
  protected readonly mesas = rxResource({ stream: () => this.finalesApi.listar() });

   protected readonly clasesDeHoy = computed<Clase[]>(() => {
    const dia = DIAS[this.hoy.getDay()];
    return (this.materias.value() ?? [])
      .filter((m) => m.periodoLectivo === 'ANUAL' || m.periodoLectivo === this.cuatrimestre)
      .flatMap((m) => m.horarios
        .filter((h) => h.diaSemana === dia)
        .map((h) => ({
          comisionMateriaId: m.comisionMateriaId,
          materia: m.nombreMateria,
          comision: m.nombreComision,
          horaInicio: h.horaInicio.slice(0, 5),
          horaFin: h.horaFin.slice(0, 5),
          aula: h.aulaNombre ?? 'Virtual',
        })))
      .sort((a, b) => a.horaInicio.localeCompare(b.horaInicio));
  });

  protected readonly proximasMesas = computed(() =>
    (this.mesas.value() ?? [])
      .filter((m) => m.fecha >= this.fechaHoy)
      .sort((a, b) => (a.fecha + a.horaInicio).localeCompare(b.fecha + b.horaInicio))
      .slice(0, 3),
  );

  protected readonly tarjetas = computed<Tarjeta[]>(() => {
    const proxima = this.proximasMesas()[0];
    return [
      {
        texto: 'Materias a cargo',
        ruta: '/docente/comisiones-materia',
        icono: 'capas',
        total: total(this.materias, (lista) => String(lista.length)),
        detalle: `en el ciclo ${this.hoy.getFullYear()}`,
      },
      {
        texto: 'Clases hoy',
        ruta: null,
        icono: 'calendario',
        total: total(this.materias, () => String(this.clasesDeHoy().length)),
        detalle: 'según tus horarios',
      },
      {
        texto: 'Próxima mesa',
        ruta: '/docente/examenes-finales',
        icono: 'documento',
        total: total(this.mesas, () => (proxima ? formatDate(proxima.fecha, 'dd/MM', 'en') : '—')),
        detalle: proxima ? proxima.nombreMateria : 'No tenés mesas asignadas',
      },
    ];
  });

  protected horarioMesa(mesa: MesaDocenteResponse): string
  {
    return `${mesa.horaInicio.slice(0, 5)} a ${mesa.horaFin.slice(0, 5)} · ${mesa.nombreAula ?? 'Virtual'}`;
  }



}

function total<T>(recurso: ResourceRef<T | undefined>, texto: (valor: T) => string): string
{
  if (recurso.hasValue()) {
    return texto(recurso.value() as T);
  }
  return recurso.error() ? '—' : '…';
}