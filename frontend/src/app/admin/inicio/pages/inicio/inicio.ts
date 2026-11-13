import { Component, computed, inject, ResourceRef } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { Sesion } from '../../../../core/seguridad/sesion';
import { Icono, NombreIcono } from '../../../../shared/ui/icono/icono';
import { CarrerasApi } from '../../../carreras/data/carreras-api';
import { DocentesApi } from '../../../docentes/data/docentes-api';
import { EstudiantesApi } from '../../../estudiantes/data/estudiantes-api';

interface Tarjeta {
  texto: string;
  ruta: string;
  icono: NombreIcono;
  /** Ya con formato: '1.234', '…' mientras carga o '—' si no se pudo pedir. */
  total: string;
  detalle: string;
}

/**
 * La pantalla a la que se entra después del login: una tarjeta por sección del menú, con cuántos
 * hay de cada cosa. No hay un endpoint de totales: cada número sale del listado de esa sección.
 */
@Component({
  selector: 'app-inicio',
  imports: [RouterLink, Icono],
  templateUrl: './inicio.html',
  styleUrl: './inicio.css',
})
export class Inicio {
  private readonly carrerasApi = inject(CarrerasApi);
  private readonly estudiantesApi = inject(EstudiantesApi);
  private readonly docentesApi = inject(DocentesApi);
  private readonly sesion = inject(Sesion);

  private readonly hoy = new Date();
  protected readonly fecha = formatearFecha(this.hoy);
  protected readonly ciclo = `Ciclo lectivo ${this.hoy.getFullYear()} · ${this.hoy.getMonth() < 7 ? '1' : '2'}.º cuatrimestre`;
  protected readonly saludo = computed(() => {
    const nombre = this.sesion.nombre();
    return nombre ? `${saludoSegunHora(this.hoy)}, ${nombre}` : saludoSegunHora(this.hoy);
  });

  /** Las carreras vienen todas juntas (son pocas): se cuentan las activas acá. */
  private readonly carreras = rxResource({ stream: () => this.carrerasApi.listarCarreras() });

  /**
   * Estudiantes y docentes vienen paginados: el total está en `page.totalElements`, sin traer a
   * todos. Para estudiantes el backend ya filtra los que tienen una inscripción activa.
   */
  private readonly estudiantes = rxResource({ stream: () => this.estudiantesApi.listar({}) });
  private readonly docentes = rxResource({
    stream: () => this.docentesApi.listarDocentes({ activo: true, size: 1 }),
  });

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
