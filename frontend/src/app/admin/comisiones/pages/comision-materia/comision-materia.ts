import { HttpErrorResponse } from '@angular/common/http';
import {
  afterNextRender,
  Component,
  computed,
  ElementRef,
  inject,
  input,
  linkedSignal,
  numberAttribute,
  signal,
  viewChild,
} from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import {
  applyEach,
  form,
  FormField,
  FormRoot,
  min,
  pattern,
  required,
} from '@angular/forms/signals';
import { ActivatedRoute, Router } from '@angular/router';
import { firstValueFrom, map } from 'rxjs';
import { ConfirmarDialog } from '../../../../shared/ui/confirmar-dialog/confirmar-dialog';
import { Icono } from '../../../../shared/ui/icono/icono';
import { AulasApi } from '../../../aulas/data/aulas-api';
import { CarrerasApi } from '../../../carreras/data/carreras-api';
import { DocenteListadoResponse } from '../../../docentes/data/docente';
import { DocentesApi } from '../../../docentes/data/docentes-api';
import { DocenteBuscador } from '../../components/docente-buscador/docente-buscador';
import { Bloque, BLOQUES } from '../../data/bloques';
import {
  ComisionMateriaResponse,
  DiaSemana,
  HorarioRequest,
  RolDocente,
} from '../../data/comision';
import { ComisionesAPi } from '../../data/comisiones-api';

/** Un docente elegido para la materia. Apellido y nombre son para mostrarlo en la ventana. */
interface DocenteElegido {
  docenteId: number;
  apellido: string;
  nombre: string;
  rol: RolDocente;
}

/** Valor del desplegable de bloques para escribir las horas a mano. */
const OTRO = 'OTRO';

/** Una hora escrita a mano, en formato de 24 hs: "14:00", "8:30" o "08:30". */
const HORA = /^([01]?\d|2[0-3]):[0-5]\d$/;

/**
 * Una fila de horario del formulario. Todo es texto porque son valores de <select> y de
 * <input type="time"> ("08:30"): '' es "sin elegir" y, en el aula, "virtual".
 * - bloque: la hora de inicio de un bloque del turno ("08:30"), u OTRO.
 * - horaInicio y horaFin: sólo se usan con OTRO.
 */
interface HorarioElegido {
  diaSemana: DiaSemana | '';
  bloque: string;
  horaInicio: string;
  horaFin: string;
  aulaId: string;
}

/** Lo que tiene el formulario. materiaId es texto porque es el valor del <select>. */
interface DatosMateria {
  materiaId: string;
  // null = sin límite.
  cupoMaximo: number | null;
  docentes: DocenteElegido[];
  horarios: HorarioElegido[];
}

const DIAS: { dia: DiaSemana; nombre: string }[] = [
  { dia: 'LUNES', nombre: 'Lunes' },
  { dia: 'MARTES', nombre: 'Martes' },
  { dia: 'MIERCOLES', nombre: 'Miércoles' },
  { dia: 'JUEVES', nombre: 'Jueves' },
  { dia: 'VIERNES', nombre: 'Viernes' },
  { dia: 'SABADO', nombre: 'Sábado' },
];

/**
 * Al agregar, todo vacío. Al editar, lo que la materia ya tiene. Las horas se cortan en "08:30"
 * por si el backend las manda con segundos. Un horario que no coincide con ningún bloque del
 * turno se abre como OTRO, con sus horas.
 */
function valoresIniciales(
  materia: ComisionMateriaResponse | undefined,
  bloques: Bloque[],
): DatosMateria {
  return {
    materiaId: materia ? String(materia.materiaId) : '',
    cupoMaximo: materia?.cupoMaximo ?? null,
    docentes: (materia?.docentes ?? []).map((docente) => ({ ...docente })),
    horarios: (materia?.horarios ?? []).map((horario) => {
      const horaInicio = horario.horaInicio.slice(0, 5);
      const horaFin = horario.horaFin.slice(0, 5);
      const bloque = bloques.find((b) => b.inicio === horaInicio && b.fin === horaFin);
      return {
        diaSemana: horario.diaSemana,
        bloque: bloque?.inicio ?? OTRO,
        horaInicio,
        horaFin,
        aulaId: horario.aulaId === null ? '' : String(horario.aulaId),
      };
    }),
  };
}

/**
 * Del formulario al request: un bloque se pasa a su hora de inicio y de fin; con OTRO van las
 * horas escritas, con el 0 adelante si hace falta ("8:30" → "08:30", como lo pide el backend).
 * Aula '' = virtual. Lo obligatorio ya está validado.
 */
function aHorarioRequest(horario: HorarioElegido, bloques: Bloque[]): HorarioRequest {
  const bloque = bloques.find((b) => b.inicio === horario.bloque);
  return {
    diaSemana: horario.diaSemana as DiaSemana,
    horaInicio: bloque?.inicio ?? horario.horaInicio.padStart(5, '0'),
    horaFin: bloque?.fin ?? horario.horaFin.padStart(5, '0'),
    aulaId: horario.aulaId === '' ? null : Number(horario.aulaId),
  };
}

/**
 * Ventana para agregar una materia a la comisión (materias/nueva) o para editarla y quitarla
 * (materias/:comisionMateriaId). Se abre encima del detalle de la comisión (ComisionDetalle), que
 * al cerrarla vuelve a pedir la comisión y muestra los cambios.
 */
@Component({
  selector: 'app-comision-materia',
  imports: [FormRoot, FormField, Icono, DocenteBuscador, ConfirmarDialog],
  templateUrl: './comision-materia.html',
  styleUrl: './comision-materia.css',
})
export class ComisionMateria {
  private readonly api = inject(ComisionesAPi);
  private readonly carrerasApi = inject(CarrerasApi);
  private readonly docentesApi = inject(DocentesApi);
  private readonly aulasApi = inject(AulasApi);
  private readonly router = inject(Router);
  private readonly ruta = inject(ActivatedRoute);
  private readonly dialogo = viewChild.required<ElementRef<HTMLDialogElement>>('dialogo');

  // carreraId y planId son de rutas de más arriba: llegan por paramsInheritanceStrategy 'always'.
  readonly carreraId = input.required({ transform: numberAttribute });
  readonly planId = input.required({ transform: numberAttribute });
  readonly comisionId = input.required({ transform: numberAttribute });
  /** Sólo al editar. En materias/nueva la ruta no tiene este parámetro y queda undefined. */
  readonly comisionMateriaId = input<number | undefined, string | undefined>(undefined, {
    transform: (valor) => (valor === undefined ? undefined : Number(valor)),
  });

  protected readonly editando = computed(() => this.comisionMateriaId() !== undefined);

  /** Lo que se hizo, para el mensaje final: "Se agregó…", "Se guardaron…", "Se quitó…". */
  protected readonly mensaje = signal<string | null>(null);
  protected readonly confirmarQuitar = signal(false);
  protected readonly cargando = signal(false);
  protected readonly error = signal<string | null>(null);

  private readonly comision = rxResource({
    params: () => this.comisionId(),
    stream: ({ params: comisionId }) => this.api.obtenerComision(comisionId),
  });

  /** La materia que se edita, buscada en el detalle de la comisión. */
  protected readonly materiaEditada = computed(() =>
    this.comision.value()?.materias.find((m) => m.id === this.comisionMateriaId()),
  );

  /** Sólo hacen falta para el desplegable del alta: al editar, la materia no se cambia. */
  private readonly materiasDelPlan = rxResource({
    params: () =>
      this.editando() ? undefined : { carreraId: this.carreraId(), planId: this.planId() },
    stream: ({ params }) => this.carrerasApi.listarMaterias(params.carreraId, params.planId),
  });

  /**
   * Las materias que se pueden agregar: las del plan que son del año de la comisión y que todavía
   * no están en ella. El backend vuelve a controlar las dos cosas.
   */
  protected readonly opciones = computed(() => {
    const comision = this.comision.value();
    if (!comision) {
      return [];
    }
    const yaEstan = new Set(comision.materias.map((m) => m.materiaId));
    return (this.materiasDelPlan.value() ?? []).filter(
      (m) => m.anioCursado === comision.anioCursado && !yaEstan.has(m.materiaId),
    );
  });

  protected readonly cargandoDatos = computed(
    () => this.comision.isLoading() || this.materiasDelPlan.isLoading(),
  );

  protected readonly errorAlCargar = computed(
    () => !!this.comision.error() || !!this.materiasDelPlan.error(),
  );

  /** Los bloques del turno de la comisión: las opciones del desplegable de cada horario. */
  protected readonly bloques = computed(() => {
    const comision = this.comision.value();
    return comision ? BLOQUES[comision.turno] : [];
  });

  /** Se vuelve a calcular cuando llega la comisión: así el formulario de edición se completa solo. */
  private readonly datos = linkedSignal(() =>
    valoresIniciales(this.materiaEditada(), this.bloques()),
  );

  protected readonly dias = DIAS;
  protected readonly otro = OTRO;

  /** Las aulas activas, para el desplegable de cada horario. */
  protected readonly aulas = rxResource({ stream: () => this.aulasApi.listar() });

  /** Lo que se escribió en el buscador de docentes. */
  private readonly busqueda = signal('');

  /** Se busca desde 2 letras. Con menos, params es undefined y el resource no pide nada. */
  private readonly encontrados = rxResource({
    params: () => {
      const texto = this.busqueda().trim();
      return texto.length >= 2 ? texto : undefined;
    },
    stream: ({ params: buscar }) =>
      this.docentesApi.listarDocentes({ buscar, size: 10 }).pipe(map((pagina) => pagina.content)),
  });

  /** Los resultados de la búsqueda, sin los docentes que ya están en la materia. */
  protected readonly opcionesDocentes = computed(() => {
    const elegidos = new Set(this.datos().docentes.map((d) => d.docenteId));
    return (this.encontrados.value() ?? []).filter((d) => !elegidos.has(d.personaId));
  });

  protected readonly materiaForm = form(
    this.datos,
    (campo) => {
      required(campo.materiaId, { message: 'Elegí la materia' });
      min(campo.cupoMaximo, 1, { message: 'El cupo tiene que ser mayor a 0' });
      // applyEach: las mismas reglas para cada horario de la lista. Las horas son obligatorias
      // sólo con OTRO (when). Que el fin sea posterior al inicio y que no haya choques lo
      // controla el backend.
      applyEach(campo.horarios, (horario) => {
        required(horario.diaSemana, { message: 'Elegí el día' });
        required(horario.bloque, { message: 'Elegí el bloque' });
        required(horario.horaInicio, {
          message: 'Poné la hora de inicio',
          when: ({ valueOf }) => valueOf(horario.bloque) === OTRO,
        });
        required(horario.horaFin, {
          message: 'Poné la hora de fin',
          when: ({ valueOf }) => valueOf(horario.bloque) === OTRO,
        });
        pattern(horario.horaInicio, HORA, {
          message: 'Usá el formato de 24 hs, por ejemplo 14:00',
          when: ({ valueOf }) => valueOf(horario.bloque) === OTRO,
        });
        pattern(horario.horaFin, HORA, {
          message: 'Usá el formato de 24 hs, por ejemplo 16:00',
          when: ({ valueOf }) => valueOf(horario.bloque) === OTRO,
        });
      });
    },
    {
      submission: {
        action: async () => {
          await this.guardar();
          return undefined;
        },
      },
    },
  );

  constructor() {
    afterNextRender(() => this.dialogo().nativeElement.showModal());
  }

  protected buscarDocente(texto: string): void {
    this.busqueda.set(texto);
  }

  /**
   * El primero que se agrega es el titular y los demás, ayudantes; el rol se puede cambiar. Que
   * haya un solo titular lo controla el backend.
   */
  protected agregarDocente(docente: DocenteListadoResponse): void {
    this.datos.update((datos) => ({
      ...datos,
      docentes: [
        ...datos.docentes,
        {
          docenteId: docente.personaId,
          apellido: docente.apellido,
          nombre: docente.nombre,
          rol: datos.docentes.some((d) => d.rol === 'TITULAR') ? 'AYUDANTE' : 'TITULAR',
        },
      ],
    }));
    this.busqueda.set('');
  }

  protected quitarDocente(indice: number): void {
    this.datos.update((datos) => ({
      ...datos,
      docentes: datos.docentes.filter((_, i) => i !== indice),
    }));
  }

  /**
   * Una fila nueva con el bloque (o las horas) y el aula de la anterior, que casi siempre se
   * repiten en otro día. Sólo falta elegir el día.
   */
  protected agregarHorario(): void {
    this.datos.update((datos) => {
      const anterior = datos.horarios.at(-1);
      return {
        ...datos,
        horarios: [
          ...datos.horarios,
          {
            diaSemana: '',
            bloque: anterior?.bloque ?? '',
            horaInicio: anterior?.horaInicio ?? '',
            horaFin: anterior?.horaFin ?? '',
            aulaId: anterior?.aulaId ?? '',
          },
        ],
      };
    });
  }

  protected quitarHorario(indice: number): void {
    this.datos.update((datos) => ({
      ...datos,
      horarios: datos.horarios.filter((_, i) => i !== indice),
    }));
  }

  /** Al agregar es un POST; al editar, un PUT que reemplaza cupo, docentes y horarios. */
  private async guardar(): Promise<void> {
    this.error.set(null);
    this.cargando.set(true);
    const datos = this.datos();
    const cambios = {
      cupoMaximo: datos.cupoMaximo,
      // Al backend van sólo el id y el rol: apellido y nombre eran para mostrarlos.
      docentes: datos.docentes.map(({ docenteId, rol }) => ({ docenteId, rol })),
      horarios: datos.horarios.map((horario) => aHorarioRequest(horario, this.bloques())),
    };
    const comisionMateriaId = this.comisionMateriaId();
    try {
      if (comisionMateriaId === undefined) {
        const materia = await firstValueFrom(
          this.api.agregarMateria(this.comisionId(), {
            materiaId: Number(datos.materiaId),
            ...cambios,
          }),
        );
        this.mensaje.set(`Se agregó ${materia.nombreMateria} a la comisión.`);
      } else {
        const materia = await firstValueFrom(
          this.api.modificarMateria(this.comisionId(), comisionMateriaId, cambios),
        );
        this.mensaje.set(`Se guardaron los cambios de ${materia.nombreMateria}.`);
      }
    } catch (error) {
      this.error.set(mensajeDeError(error));
    }
    this.cargando.set(false);
  }

  /** Si la materia ya tiene inscripciones, evaluaciones o encuestas, el backend no deja (409). */
  protected async quitar(): Promise<void> {
    const materia = this.materiaEditada();
    if (!materia) {
      return;
    }
    this.error.set(null);
    this.cargando.set(true);
    try {
      await firstValueFrom(this.api.quitarMateria(this.comisionId(), materia.id));
      this.mensaje.set(`Se quitó ${materia.nombreMateria} de la comisión.`);
    } catch (error) {
      this.error.set(mensajeDeError(error));
    }
    this.confirmarQuitar.set(false);
    this.cargando.set(false);
  }

  protected cerrar(): void {
    this.dialogo().nativeElement.close();
  }

  /** La ventana está en comisiones/:comisionId/materias/...: "../.." es el detalle. */
  protected volver(): void {
    void this.router.navigate(['../..'], { relativeTo: this.ruta });
  }
}

function mensajeDeError(error: unknown): string {
  if (error instanceof HttpErrorResponse && error.status !== 0 && error.status !== 500) {
    return error.error?.errores?.[0]?.mensaje ?? error.error?.detail ?? 'Ocurrió un error';
  }
  return 'Ocurrió un error';
}
