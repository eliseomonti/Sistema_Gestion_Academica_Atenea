import { Component, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { form, FormField } from '@angular/forms/signals';
import { Celda } from '../../../../shared/ui/tabla/celda';
import { Columna } from '../../../../shared/ui/tabla/columna';
import { Tabla } from '../../../../shared/ui/tabla/tabla';
import { Etiqueta } from '../../../../shared/ui/etiqueta/etiqueta';
import { Icono } from '../../../../shared/ui/icono/icono';
import { CarreraResponse, ModalidadCarrera } from '../../data/carrera';
import { CarrerasApi } from '../../data/carreras-api';

/** Opciones del filtro por estado. */
type FiltroEstado = 'TODAS' | 'ACTIVAS' | 'INACTIVAS';

const MODALIDADES: Record<ModalidadCarrera, string> = {
  PRESENCIAL: 'Presencial',
  VIRTUAL: 'Virtual',
};

@Component({
  selector: 'app-carreras-lista',
  imports: [FormField, Tabla, Celda, Etiqueta, Icono],
  templateUrl: './carreras-lista.html',
  styleUrl: './carreras-lista.css',
})
export class CarrerasLista {
  private readonly api = inject(CarrerasApi);

  /** Pide las carreras al abrir la pantalla. Da el valor y los estados de cargando y error. */
  protected readonly carreras = rxResource({ stream: () => this.api.listarCarreras() });

  /** El buscador y el desplegable de estado escriben acá, a través de `filtrosForm`. */
  private readonly filtros = signal<{ busqueda: string; estado: FiltroEstado }>({
    busqueda: '',
    estado: 'TODAS',
  });
  protected readonly filtrosForm = form(this.filtros);

  /** Hay algún filtro aplicado además de la búsqueda: el botón de filtro se pinta. */
  protected readonly hayFiltro = computed(() => this.filtros().estado !== 'TODAS');

  /** Son pocas carreras: se filtran en el navegador, sin volver a llamar a la API. */
  protected readonly carrerasFiltradas = computed(() => {
    const { busqueda, estado } = this.filtros();
    const texto = normalizar(busqueda);
    return (this.carreras.value() ?? []).filter(
      (carrera) =>
        normalizar(carrera.nombre).includes(texto) && cumpleEstado(carrera.activa, estado),
    );
  });

  protected readonly columnas: Columna<CarreraResponse>[] = [
    { clave: 'nombre', titulo: 'Carrera' },
    { clave: 'modalidad', titulo: 'Modalidad', valor: (c) => MODALIDADES[c.modalidad] },
    { clave: 'activa', titulo: 'Estado' }, // la muestra el <ng-template appCelda="activa">
  ];

  /** Cada fila lleva a los planes de la carrera: /admin/carreras/:id/planes. */
  protected readonly enlace = (carrera: CarreraResponse) => [carrera.id, 'planes'];
}

function cumpleEstado(activa: boolean, estado: FiltroEstado): boolean {
  if (estado === 'ACTIVAS') {
    return activa;
  }
  if (estado === 'INACTIVAS') {
    return !activa;
  }
  return true;
}

/** Minúsculas y sin tildes, para que "ingenieria" encuentre "Ingeniería". */
function normalizar(texto: string): string {
  return texto
    .normalize('NFD')
    .replace(/\p{Diacritic}/gu, '')
    .toLowerCase()
    .trim();
}
