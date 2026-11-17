import { DatePipe, formatDate } from '@angular/common';
import { Component, computed, inject, ResourceRef } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { forkJoin, map, of } from 'rxjs';
import { Sesion } from '../../../../core/seguridad/sesion';
import { Bienvenida } from '../../../../shared/ui/bienvenida/bienvenida';
import { Icono, NombreIcono } from '../../../../shared/ui/icono/icono';
import { CarrerasApi } from '../../../carreras/data/carreras-api';
import { DocentesApi } from '../../../docentes/data/docentes-api';
import { EstudiantesApi } from '../../../estudiantes/data/estudiantes-api';
import { ExamenesFinalesApi } from '../../../examenes-finales/data/examen-final-api';
import { ExamenFinalResponse } from '../../../examenes-finales/data/examen-final';
import { PeriodoInscripcionResponse } from '../../../periodos-inscripcion/data/periodo-inscripcion';
import { PeriodoInscripcionApi } from '../../../periodos-inscripcion/data/periodos-inscripcion-api';

interface Tarjeta {
  texto: string;
  ruta: string;
  icono: NombreIcono;
  /** Ya con formato: '1.234', '…' mientras carga o '—' si no se pudo pedir. */
  total: string;
  detalle: string;
}

/** Un periodo con la carrera a la que pertenece y el link para abrirlo. */
interface PeriodoDeCarrera {
  periodo: PeriodoInscripcionResponse;
  carrera: string;
  ruta: unknown[] | null;
}

/** Una mesa con la carrera de su turno y el link para abrirla. */
interface MesaDeCarrera {
  mesa: ExamenFinalResponse;
  carrera: string;
  ruta: unknown[] | null;
}

/** Cuántos días para adelante se muestran las inscripciones que todavía no abrieron. */
const DIAS_ANTICIPACION = 30;

/**
 * La pantalla a la que se entra después del login: una tarjeta por sección del menú, con cuántos
 * hay de cada cosa. No hay un endpoint de totales: cada número sale del listado de esa sección.
 */
@Component({
  selector: 'app-inicio',
  imports: [RouterLink, Bienvenida, Icono, DatePipe],
  templateUrl: './inicio.html',
  styleUrl: './inicio.css',
})
export class Inicio {
  private readonly carrerasApi = inject(CarrerasApi);
  private readonly estudiantesApi = inject(EstudiantesApi);
  private readonly docentesApi = inject(DocentesApi);
  private readonly periodosApi = inject(PeriodoInscripcionApi);
  private readonly finalesApi = inject(ExamenesFinalesApi);
  protected readonly sesion = inject(Sesion);

  /** Las carreras vienen todas juntas (son pocas): se cuentan las activas acá. */
  protected readonly carreras = rxResource({ stream: () => this.carrerasApi.listarCarreras() });

  /**
   * Estudiantes y docentes vienen paginados: el total está en `page.totalElements`, sin traer a
   * todos. Para estudiantes el backend ya filtra los que tienen una inscripción activa.
   */
  private readonly estudiantes = rxResource({ stream: () => this.estudiantesApi.listar({}) });
  private readonly docentes = rxResource({
    stream: () => this.docentesApi.listarDocentes({ activo: true, size: 1 }),
  });

  private readonly hoy = new Date();
  private readonly fechaHoy = formatDate(this.hoy, 'yyyy-MM-dd', 'en');

  /**
   * Los periodos del año de cada carrera activa. No hay un endpoint que los traiga todos juntos:
   * se pide uno por carrera, junto con sus planes, porque las pantallas del periodo y de la mesa
   * están dentro de un plan (se usa el vigente).
   */
  protected readonly periodos = rxResource({
    params: () => (this.carreras.hasValue() ? this.carreras.value().filter((c) => c.activa) : undefined),
    stream: ({ params: carreras }) =>
      carreras.length === 0
        ? of([])
        : forkJoin(
            carreras.map((carrera) =>
              forkJoin({
                planes: this.carrerasApi.listarPlanes(carrera.id),
                periodos: this.periodosApi.listar({ carreraId: carrera.id, anioLectivo: this.hoy.getFullYear() }),
              }).pipe(
                map(({ planes, periodos }) => {
                  const plan = planes.find((p) => p.vigente) ?? planes[0];
                  return periodos.map<PeriodoDeCarrera>((periodo) => ({
                    periodo,
                    carrera: carrera.nombre,
                    ruta: plan
                      ? ['/admin/carreras', carrera.id, 'planes', plan.id, 'periodos', periodo.id]
                      : null,
                  }));
                }),
              ),
            ),
          ).pipe(map((listas) => listas.flat())),
  });

  /** Habilitadas y abiertas hoy, o que abren en los próximos días, de la que abre antes. */
  protected readonly inscripcionesAbiertas = computed(() => {
    const ahora = this.hoy.getTime();
    const limite = ahora + DIAS_ANTICIPACION * 24 * 60 * 60 * 1000;
    return (this.periodos.value() ?? [])
      .filter(({ periodo }) => periodo.habilitada)
      .filter(({ periodo }) => new Date(periodo.fechaCierre).getTime() >= ahora)
      .filter(({ periodo }) => new Date(periodo.fechaInicio).getTime() <= limite)
      .sort((a, b) => a.periodo.fechaInicio.localeCompare(b.periodo.fechaInicio))
      .slice(0, 5);
  });

  /** Las mesas de todos los turnos de final del año: un pedido por turno. */
  protected readonly mesas = rxResource({
    params: () =>
      this.periodos.hasValue()
        ? this.periodos.value().filter(({ periodo }) => periodo.tipo === 'FINAL')
        : undefined,
    stream: ({ params: turnos }) =>
      turnos.length === 0
        ? of([])
        : forkJoin(
            turnos.map(({ periodo, carrera, ruta }) =>
              this.finalesApi.listar(periodo.id).pipe(
                map((mesas) =>
                  mesas.map<MesaDeCarrera>((mesa) => ({
                    mesa,
                    carrera,
                    ruta: ruta ? [...ruta, 'mesas', mesa.id] : null,
                  })),
                ),
              ),
            ),
          ).pipe(map((listas) => listas.flat())),
  });

  /** Desde hoy, de la más cercana a la más lejana. */
  protected readonly proximasMesas = computed(() =>
    (this.mesas.value() ?? [])
      .filter(({ mesa }) => mesa.fecha >= this.fechaHoy)
      .sort((a, b) => (a.mesa.fecha + a.mesa.horaInicio).localeCompare(b.mesa.fecha + b.mesa.horaInicio))
      .slice(0, 5),
  );

  protected abierta(periodo: PeriodoInscripcionResponse): boolean {
    return new Date(periodo.fechaInicio).getTime() <= this.hoy.getTime();
  }

  protected tipoPeriodo(periodo: PeriodoInscripcionResponse): string {
    if (periodo.tipo === 'FINAL') {
      return 'Finales';
    }
    const cuatrimestre: Record<string, string> = {
      PRIMER_CUATRIMESTRE: '1er cuatrimestre',
      SEGUNDO_CUATRIMESTRE: '2do cuatrimestre',
      ANUAL: 'anual',
    };
    return `Cursada ${periodo.periodoLectivo ? cuatrimestre[periodo.periodoLectivo] : ''}`.trim();
  }

  protected horario(mesa: ExamenFinalResponse): string {
    return `${mesa.horaInicio.slice(0, 5)} a ${mesa.horaFin.slice(0, 5)} · ${mesa.nombreAula ?? 'Virtual'}`;
  }

  protected readonly tarjetas = computed<Tarjeta[]>(() => [
    {
      texto: 'Carreras',
      ruta: '/admin/carreras',
      icono: 'libro',
      total: total(this.carreras, (carreras) => carreras.filter((c) => c.activa).length),
      detalle: 'activas',
    },
    {
      texto: 'Estudiantes',
      ruta: '/admin/estudiantes',
      icono: 'birrete',
      total: total(this.estudiantes, (pagina) => pagina.page.totalElements),
      detalle: 'activos',
    },
    {
      texto: 'Docentes',
      ruta: '/admin/docentes',
      icono: 'persona',
      total: total(this.docentes, (pagina) => pagina.page.totalElements),
      detalle: 'activos',
    },
  ]);
}

/** El número de una tarjeta, con punto de miles. hasValue() evita leer value() si hubo error. */
function total<T>(recurso: ResourceRef<T | undefined>, contar: (valor: T) => number): string {
  if (recurso.hasValue()) {
    return contar(recurso.value() as T).toLocaleString('es-AR');
  }
  return recurso.error() ? '—' : '…';
}
