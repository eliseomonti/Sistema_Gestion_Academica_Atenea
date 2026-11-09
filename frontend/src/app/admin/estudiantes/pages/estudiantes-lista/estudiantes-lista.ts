import { Component, computed, inject, input, linkedSignal, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { debounce, form, FormField } from '@angular/forms/signals';
import { RouterLink, RouterOutlet } from '@angular/router';
import { Pagina } from '../../../../shared/models/pagina';
import { Paginador } from '../../../../shared/ui/paginador/paginador';
import { Columna } from '../../../../shared/ui/tabla/columna';
import { Icono } from '../../../../shared/ui/icono/icono';
import { Tabla } from '../../../../shared/ui/tabla/tabla';
import { CarrerasApi } from '../../../carreras/data/carreras-api';
import { EstudianteListadoResponse } from '../../data/estudiante';
import { EstudiantesApi } from '../../data/estudiantes-api';

type ListadoEstudiantes = Pagina<EstudianteListadoResponse>;

/**
 * Los estudiantes activos, de a 20. A diferencia de carreras, pueden ser miles: la búsqueda y el
 * cambio de página los resuelve el backend, que devuelve sólo la página pedida.
 *
 * Se usa en dos lugares: en /admin/estudiantes, con todos, y como pestaña Estudiantes de la
 * pantalla del plan, sólo con los de ese plan.
 */
@Component({
  selector: 'app-estudiantes-lista',
  imports: [FormField, RouterLink, RouterOutlet, Icono, Tabla, Paginador],
  templateUrl: './estudiantes-lista.html',
  styleUrl: './estudiantes-lista.css',
})
export class EstudiantesLista {
  private readonly api = inject(EstudiantesApi);
  private readonly carrerasApi = inject(CarrerasApi);

  /**
   * El :planId de la URL, cuando la pantalla es la pestaña de un plan. En /admin/estudiantes no hay
   * plan y llega undefined: por eso no se usa numberAttribute, que lo convertiría en NaN.
   */
  readonly planId = input(undefined, {
    transform: (valor: string | undefined) => (valor === undefined ? undefined : Number(valor)),
  });

  /**
   * Las carreras del desplegable. Sólo en /admin/estudiantes: en la pestaña de un plan la carrera
   * ya está elegida, y con params undefined no se piden.
   */
  private readonly carreras = rxResource({
    params: () => (this.planId() === undefined ? {} : undefined),
    stream: () => this.carrerasApi.listarCarreras(),
  });
  protected readonly opcionesCarrera = computed(() =>
    this.carreras.hasValue() ? this.carreras.value() : [],
  );

  /**
   * El buscador y el desplegable de carrera escriben acá, a través de `filtrosForm`. carreraId es
   * texto porque es el valor de un <select>: '' es "Todas las carreras".
   */
  private readonly filtros = signal({ busqueda: '', carreraId: '' });

  /** Hay algún filtro puesto: cambia el mensaje de la lista vacía. */
  protected readonly hayFiltro = computed(() => {
    const { busqueda, carreraId } = this.filtros();
    return busqueda.trim() !== '' || carreraId !== '';
  });

  /**
   * debounce: el texto pasa a `filtros` recién cuando se deja de escribir por 300 ms. Sin esto,
   * cada letra sería un pedido al backend.
   */
  protected readonly filtrosForm = form(this.filtros, (campos) => {
    debounce(campos.busqueda, 300);
  });

  /**
   * La página que se muestra, empezando en 0. linkedSignal: se puede cambiar con set() (el
   * paginador), y vuelve sola a 0 cuando cambia un filtro, porque los resultados son otros.
   */
  protected readonly pagina = linkedSignal({
    source: () => this.filtros(),
    computation: () => 0,
  });

  /** Se vuelve a pedir cada vez que cambia un filtro o la página. */
  protected readonly estudiantes = rxResource({
    params: () => ({
      buscar: this.filtros().busqueda,
      carreraId: this.filtros().carreraId === '' ? undefined : Number(this.filtros().carreraId),
      planEstudioId: this.planId(),
      pagina: this.pagina(),
    }),
    stream: ({ params }) => this.api.listar(params),
  });

  /**
   * La última respuesta que llegó. Mientras se pide otra página, `estudiantes.value()` queda vacío:
   * con esto la tabla sigue mostrando la anterior en vez de desaparecer y volver a aparecer.
   */
  protected readonly listado = linkedSignal<
    ListadoEstudiantes | undefined,
    ListadoEstudiantes | undefined
  >({
    source: () => (this.estudiantes.hasValue() ? this.estudiantes.value() : undefined),
    computation: (actual, anterior) => actual ?? anterior?.value,
  });

  protected readonly columnas: Columna<EstudianteListadoResponse>[] = [
    { clave: 'legajo', titulo: 'Legajo', tituloEnLista: true },
    {
      clave: 'nombre',
      titulo: 'Estudiante',
      valor: (e) => `${e.apellido}, ${e.nombre}`,
      principal: true,
    },
    { clave: 'dni', titulo: 'DNI', tituloEnLista: true },
    { clave: 'email', titulo: 'Email' },
  ];
}
