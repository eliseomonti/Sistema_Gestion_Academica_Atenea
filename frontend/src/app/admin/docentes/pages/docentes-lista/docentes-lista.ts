import { Component, computed, inject, linkedSignal, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { debounce, form, FormField } from '@angular/forms/signals';
import { RouterLink, RouterOutlet } from '@angular/router';
import { Pagina } from '../../../../shared/models/pagina';
import { Icono } from '../../../../shared/ui/icono/icono';
import { Paginador } from '../../../../shared/ui/paginador/paginador';
import { Columna } from '../../../../shared/ui/tabla/columna';
import { Tabla } from '../../../../shared/ui/tabla/tabla';
import { DocenteListadoResponse } from '../../data/docente';
import { DocentesApi } from '../../data/docentes-api';

type ListadoDocentes = Pagina<DocenteListadoResponse>;

/** Opciones del filtro por estado. */
type FiltroEstado = 'ACTIVOS' | 'INACTIVOS';

/**
 * Los docentes, de a 20, igual que la lista de estudiantes: la búsqueda y el cambio de página los
 * resuelve el backend, que devuelve sólo la página pedida.
 */
@Component({
  selector: 'app-docentes-lista',
  imports: [FormField, RouterLink, RouterOutlet, Icono, Tabla, Paginador],
  templateUrl: './docentes-lista.html',
  styleUrl: './docentes-lista.css',
})
export class DocentesLista {
  private readonly api = inject(DocentesApi);
  protected readonly enlace = (docente: DocenteListadoResponse) => [docente.personaId];

  /** El buscador y el desplegable de estado escriben acá, a través de `filtrosForm`. */
  private readonly filtros = signal<{ busqueda: string; estado: FiltroEstado }>({
    busqueda: '',
    estado: 'ACTIVOS',
  });

  /**
   * debounce: el texto pasa a `filtros` recién cuando se deja de escribir por 300 ms. Sin esto,
   * cada letra sería un pedido al backend.
   */
  protected readonly filtrosForm = form(this.filtros, (campos) => {
    debounce(campos.busqueda, 300);
  });

  /** Se ven los inactivos: el botón de filtro se pinta, porque la lista no es la de siempre. */
  protected readonly hayFiltro = computed(() => this.filtros().estado !== 'ACTIVOS');

  /**
   * La página que se muestra, empezando en 0. linkedSignal: se puede cambiar con set() (el
   * paginador), y vuelve sola a 0 cuando cambia un filtro, porque los resultados son otros.
   */
  protected readonly pagina = linkedSignal({
    source: () => this.filtros(),
    computation: () => 0,
  });

  /** Se vuelve a pedir cada vez que cambia un filtro o la página. */
  protected readonly docentes = rxResource({
    params: () => ({
      buscar: this.filtros().busqueda.trim(),
      activo: this.filtros().estado === 'ACTIVOS',
      page: this.pagina(),
    }),
    stream: ({ params }) => this.api.listarDocentes(params),
  });

  /**
   * La última respuesta que llegó. Mientras se pide otra página, `docentes.value()` queda vacío:
   * con esto la tabla sigue mostrando la anterior en vez de desaparecer y volver a aparecer.
   */
  protected readonly listado = linkedSignal<ListadoDocentes | undefined, ListadoDocentes | undefined>({
    source: () => (this.docentes.hasValue() ? this.docentes.value() : undefined),
    computation: (actual, anterior) => actual ?? anterior?.value,
  });

  protected readonly columnas: Columna<DocenteListadoResponse>[] = [
    { clave: 'legajo', titulo: 'Legajo', tituloEnLista: true },
    {
      clave: 'docente',
      titulo: 'Docente',
      valor: (d) => `${d.apellido}, ${d.nombre}`,
      principal: true,
    },
    { clave: 'dni', titulo: 'DNI', tituloEnLista: true },
    { clave: 'email', titulo: 'Email' },
  ];
}
