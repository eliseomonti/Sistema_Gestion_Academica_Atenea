import { Component, computed, inject, ResourceRef } from '@angular/core';
import { Icono, NombreIcono } from '../../../../shared/ui/icono/icono';
import { Sesion } from '../../../../core/seguridad/sesion';
import { InscripcionesComisionApi } from '../../../inscripciones-comision/data/inscripciones-comision-api';
import { rxResource } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { Bienvenida } from '../../../../shared/ui/bienvenida/bienvenida';
import { NotasApi } from '../../../notas/data/notas-api';
import { PlanEstudioApi } from '../../../plan-estudio/data/plan-estudio-api';
import { DatePipe } from '@angular/common';
import { InscripcionFinalApi } from '../../../inscripciones-final/data/inscripciones-final-api';
import { InscripcionFinalResponse } from '../../../inscripciones-final/data/inscripcion-final';


interface Tarjeta {
  texto: string;
  ruta: string | null;
  icono: NombreIcono;
  total: string;
  detalle: string;
}

interface NotaReciente
{
  materia: string;
  detalle: string;
  fecha: string;
  nota: number;
}

@Component({
  imports: [RouterLink, Bienvenida, Icono, DatePipe],
  selector: 'app-inicio',
  styleUrl: './inicio.css',
  templateUrl: './inicio.html',
})
export class Inicio {

  private readonly inscripcionesApi = inject(InscripcionesComisionApi);
  protected readonly sesion = inject(Sesion);
  private readonly notasApi =inject(NotasApi)
  private readonly planApi = inject(PlanEstudioApi);
  private readonly finalesApi = inject(InscripcionFinalApi);

  private readonly inscripciones = rxResource({ stream: () => this.inscripcionesApi.listarInscripciones() });
  protected readonly finales = rxResource({ stream: () => this.notasApi.listarFinales() });
  private readonly planes = rxResource({ stream: () => this.planApi.listarPlan() });



  private readonly oferta = rxResource({
    stream: () => this.inscripcionesApi.disponibilidad(),
  });


  protected readonly tarjetas = computed<Tarjeta[]>(() => [
    {
      texto: 'Materias inscriptas',
      ruta: '/estudiante/inscripciones-comision',
      icono: 'libro',
      total: total(this.inscripciones, (lista) => String(lista.filter((i) => i.estado === 'INSCRIPTO').length)),
      detalle: 'en este ciclo lectivo',
    },
    {
      texto: 'Finales aprobados',
      ruta: null,
      icono: 'medalla',
      total: total(this.finales, (lista) => String(lista.filter((f) => f.aprobado === true).length)),
      detalle: 'en toda la carrera',
    },
    {
      texto: 'Avance de la carrera',
      ruta: null,
      icono: 'birrete',
      total: total(this.planes, (planes) => `${planes[0]?.porcentajeAvance ?? 0}%`),
      detalle: 'de las materias aprobadas',
    },
    {
      texto: 'Para inscribirte',
      ruta: '/estudiante/inscripciones-comision',
      icono: 'editar',
      total: total(this.oferta, (lista) => String(lista.length)),
      detalle: 'materias disponibles',
    },
  ]);


  protected readonly mesas = rxResource({ stream: () => this.finalesApi.listar() })
  protected readonly proximaMesa = computed(() => (this.mesas.value() ?? [])
  .filter((m) => m.estado === 'INSCRIPTO')
  .sort((a,b) => (a.fecha + a.horaInicio).localeCompare(b.fecha + b.horaInicio))
  .slice(0,3)  
)

  protected horario(mesa: InscripcionFinalResponse): string
  {
    const aula = mesa.nombreAula ?? 'Virtual';
    return `${mesa.horaInicio.slice(0,5)} a ${mesa.horaFin.slice(0,5)} ${aula}`;
  }

  protected readonly cursadas = rxResource({ stream: () => this.notasApi.listarCursada() });

  protected readonly notasRecientes = computed<NotaReciente[]>(() => {
    const deCursado = (this.cursadas.value() ?? []).flatMap((cursada) => 
    cursada.evaluaciones
    .filter((e) => e.nota !== null)
    .map((e) => ({ materia: cursada.nombreMateria, detalle: e.nombre, fecha: e.fecha, nota: e.nota as number }))
  )

  const deFinal = (this.finales.value() ?? [])
    .filter((f) => f.nota !== null)
    .map((f) => ({ materia: f.nombreMateria, detalle: 'Final', fecha: f.fecha, nota: f.nota as number }));

    return [...deCursado, ...deFinal]
      .sort((a, b) => b.fecha.localeCompare(a.fecha))
      .slice(0, 3);
  })
  

}
function total<T>(recurso: ResourceRef<T | undefined>, texto: (valor: T) => string): string
{
  if (recurso.hasValue()) {
    return texto(recurso.value() as T);
  }
  return recurso.error() ? '—' : '…';
}