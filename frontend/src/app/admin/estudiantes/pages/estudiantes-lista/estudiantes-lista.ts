import { Component, inject, linkedSignal, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { debounce, form, FormField } from '@angular/forms/signals';
import { Pagina } from '../../../../shared/models/pagina';
import { Paginador } from '../../../../shared/ui/paginador/paginador';
import { Columna } from '../../../../shared/ui/tabla/columna';
import { Tabla } from '../../../../shared/ui/tabla/tabla';
import { EstudianteListadoResponse } from '../../data/estudiante';
import { EstudiantesApi } from '../../data/estudiantes-api';

type ListadoEstudiantes = Pagina<EstudianteListadoResponse>;

/**
 * Los estudiantes activos, de a 20. A diferencia de carreras, pueden ser miles: la búsqueda y el
 * cambio de página los resuelve el backend, que devuelve sólo la página pedida.
 */
@Component({
  selector: 'app-estudiantes-lista',
  imports: [FormField, Tabla, Paginador],
  templateUrl: './estudiantes-lista.html',
  styleUrl: './estudiantes-lista.css',
})
export class EstudiantesLista {
  private readonly api = inject(EstudiantesApi);

  /** El buscador escribe acá, a través de `filtrosForm`. */
  private readonly filtros = signal({ busqueda: '' });

  /**
   * debounce: el texto pasa a `filtros` recién cuando se deja de escribir por 300 ms. Sin esto,
   * cada letra sería un pedido al backend.
   */
  protected readonly filtrosForm = form(this.filtros, (campos) => {
    debounce(campos.busqueda, 300);
  });

  /**
   * La página que se muestra, empezando en 0. linkedSignal: se puede cambiar con set() (el
   * paginador), y vuelve sola a 0 cuando cambia la búsqueda, porque los resultados son otros.
   */
  protected readonly pagina = linkedSignal({
    source: () => this.filtros().busqueda,
    computation: () => 0,
  });

  /** Se vuelve a pedir cada vez que cambia la búsqueda o la página. */
  protected readonly estudiantes = rxResource({
    params: () => ({ buscar: this.filtros().busqueda, pagina: this.pagina() }),
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
