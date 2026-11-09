import { HttpErrorResponse } from '@angular/common/http';
import {
  afterNextRender,
  Component,
  computed,
  effect,
  ElementRef,
  inject,
  input,
  signal,
  viewChild,
} from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { disabled, form, FormField, FormRoot, required } from '@angular/forms/signals';
import { ActivatedRoute, Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { Icono } from '../../../../shared/ui/icono/icono';
import { CarrerasApi } from '../../../carreras/data/carreras-api';
import { VerificacionInscripcionResponse } from '../../../estudiantes/data/estudiante';
import { EstudiantesApi } from '../../../estudiantes/data/estudiantes-api';
import { DatosPersonalesForm } from '../../components/datos-personales-form/datos-personales-form';
import { DniPaso } from '../../components/dni-paso/dni-paso';
import { AltaPersonaResponse, DatosPersonalesRequest, PersonaResponse } from '../../data/persona';
import { PersonasApi } from '../../data/personas-api';

/**
 * Ventana para inscribir a alguien en un plan, sobre la lista de estudiantes (es una ruta hija de
 * la lista). Va por pasos, como el alta de docente:
 * - En /admin/estudiantes no hay plan en la URL: primero se elige la carrera y el plan. En la
 *   pestaña de un plan, este paso no aparece.
 * - Después se busca el DNI. Si la persona ya existe (estudiante de otra carrera, o docente), sólo
 *   se la inscribe. Si no existe, se cargan sus datos: el alta la deja inscripta y se muestran su
 *   usuario y su contraseña.
 */
@Component({
  selector: 'app-estudiante-inscripcion',
  imports: [FormRoot, FormField, DniPaso, DatosPersonalesForm, Icono],
  templateUrl: './estudiante-inscripcion.html',
  styleUrl: './estudiante-inscripcion.css',
})
export class EstudianteInscripcion {
  private readonly personasApi = inject(PersonasApi);
  private readonly estudiantesApi = inject(EstudiantesApi);
  private readonly carrerasApi = inject(CarrerasApi);
  private readonly router = inject(Router);
  private readonly ruta = inject(ActivatedRoute);

  /**
   * El :planId de la URL, en la pestaña de un plan. En /admin/estudiantes llega undefined: por eso
   * no se usa numberAttribute, que lo convertiría en NaN.
   */
  readonly planId = input(undefined, {
    transform: (valor: string | undefined) => (valor === undefined ? undefined : Number(valor)),
  });

  /** La carrera y el plan del primer paso, sólo en /admin/estudiantes. Son el valor de un <select>. */
  private readonly eleccion = signal({ carreraId: '', planId: '' });
  /** El plan elegido en el primer paso, cuando se tocó Siguiente. */
  private readonly planElegido = signal<number | null>(null);

  protected readonly eleccionForm = form(
    this.eleccion,
    (campo) => {
      required(campo.carreraId, { message: 'Elegí la carrera.' });
      disabled(campo.planId, () => this.eleccion().carreraId === '');
      required(campo.planId, { message: 'Elegí el plan de estudio.' });
    },
    {
      submission: {
        action: async () => {
          this.planElegido.set(Number(this.eleccion().planId));
          return undefined;
        },
      },
    },
  );

  /** El plan en el que se inscribe: el de la URL o el elegido. Sin plan, se muestra el primer paso. */
  protected readonly planEstudioId = computed(() => this.planId() ?? this.planElegido());

  /** Sólo las carreras activas y sus planes vigentes: son los que aceptan inscripciones. */
  private readonly carreras = rxResource({
    params: () => (this.planId() === undefined ? {} : undefined), // con plan en la URL no se piden
    stream: () => this.carrerasApi.listarCarreras(),
  });
  protected readonly carrerasActivas = computed(() =>
    this.carreras.hasValue() ? this.carreras.value().filter((carrera) => carrera.activa) : [],
  );
  /**
   * La carrera elegida, aparte de `eleccion`. rxResource vuelve a pedir cada vez que cambia algo de
   * lo que lee en params, aunque dé lo mismo: si leyera `eleccion`, elegir el plan volvería a pedir
   * los planes y el effect de abajo borraría lo elegido. El computed sólo avisa si cambia la carrera.
   */
  private readonly carreraElegida = computed(() => this.eleccion().carreraId);
  private readonly planes = rxResource({
    params: () => this.carreraElegida() || undefined, // sin carrera elegida no se piden
    stream: ({ params: carreraId }) => this.carrerasApi.listarPlanes(Number(carreraId)),
  });
  protected readonly planesVigentes = computed(() =>
    this.planes.hasValue() ? this.planes.value().filter((plan) => plan.vigente) : [],
  );
  /** Ya se sabe que la carrera elegida no tiene planes vigentes: no se puede inscribir en ella. */
  protected readonly sinPlanesVigentes = computed(
    () => this.planes.hasValue() && this.planesVigentes().length === 0,
  );

  /** Arriba de los pasos siguientes: "Ingeniería en Sistemas · Plan 2023". Sólo si se eligió. */
  protected readonly contexto = computed(() => {
    if (this.planElegido() === null) {
      return null;
    }
    const { carreraId, planId } = this.eleccion();
    const carrera = this.carrerasActivas().find((c) => c.id === Number(carreraId));
    const plan = this.planesVigentes().find((p) => p.id === Number(planId));
    return `${carrera?.nombre} · ${plan?.nombre}`;
  });

  private readonly dialogo = viewChild.required<ElementRef<HTMLDialogElement>>('dialogo');

  /** El DNI buscado, cuando no es de nadie: pasa al paso de los datos personales. */
  protected readonly dniNuevo = signal<string | null>(null);
  protected readonly personaExistente = signal<PersonaResponse | null>(null);
  /** Si la persona que ya existe se puede inscribir en este plan; si no, el motivo. */
  protected readonly verificacion = signal<VerificacionInscripcionResponse | null>(null);
  protected readonly credenciales = signal<AltaPersonaResponse | null>(null);
  /** A quién se inscribió, para el mensaje de éxito. Con esto la ventana pasa al último paso. */
  protected readonly inscripta = signal<string | null>(null);
  protected readonly cargando = signal(false);
  protected readonly error = signal<string | null>(null);

  constructor() {
    // showModal() la abre como ventana: oscurece la lista y deja el foco del teclado adentro.
    afterNextRender(() => this.dialogo().nativeElement.showModal());

    // Cada vez que llegan los planes de otra carrera, el plan elegido antes deja de valer. Si hay
    // uno solo vigente, queda elegido.
    effect(() => {
      const vigentes = this.planesVigentes();
      const planId = vigentes.length === 1 ? String(vigentes[0].id) : '';
      this.eleccion.update((eleccion) => ({ ...eleccion, planId }));
    });
  }

  /** El plan en el que se inscribe. Los pasos que lo usan sólo se muestran cuando ya hay uno. */
  private plan(): number {
    return this.planEstudioId() as number;
  }

  /**
   * El backend responde 404 si nadie tiene ese DNI: en ese caso se pasa al alta. Si la persona
   * existe, también se pregunta si se puede inscribir, para no ofrecer un botón que va a fallar.
   */
  protected async buscar(dni: string): Promise<void> {
    this.error.set(null);
    this.personaExistente.set(null);
    this.verificacion.set(null);
    this.cargando.set(true);
    try {
      const persona = await firstValueFrom(this.personasApi.buscarPorDni(dni));
      const verificacion = this.estudiantesApi.verificarInscripcion(persona.id, this.plan());
      this.verificacion.set(await firstValueFrom(verificacion));
      this.personaExistente.set(persona); // al final: así aparece junto con el botón o el motivo
    } catch (error) {
      if (error instanceof HttpErrorResponse && error.status === 404) {
        this.dniNuevo.set(dni);
      } else {
        this.error.set(mensajeDeError(error));
      }
    }
    this.cargando.set(false);
  }

  /** Cancelar en la pregunta: vuelve al paso del DNI, para buscar otro. */
  protected volverAlDni(): void {
    this.error.set(null);
    this.personaExistente.set(null);
    this.verificacion.set(null);
  }

  /** Alta de la persona nueva, que queda inscripta en el plan. */
  protected async guardar(datos: DatosPersonalesRequest): Promise<void> {
    this.error.set(null);
    this.cargando.set(true);
    try {
      const alta = this.estudiantesApi.alta({ datosPersonales: datos, planEstudioId: this.plan() });
      this.credenciales.set(await firstValueFrom(alta));
      this.inscripta.set(`${datos.nombre} ${datos.apellido}`);
    } catch (error) {
      this.error.set(mensajeDeError(error));
    }
    this.cargando.set(false);
  }

  /** Inscribe en el plan a la persona que ya existía. */
  protected async inscribir(persona: PersonaResponse): Promise<void> {
    this.error.set(null);
    this.cargando.set(true);
    try {
      await firstValueFrom(
        this.estudiantesApi.inscribir(persona.id, { planEstudioId: this.plan() }),
      );
      this.inscripta.set(`${persona.nombre} ${persona.apellido}`);
    } catch (error) {
      this.error.set(mensajeDeError(error));
    }
    this.cargando.set(false);
  }

  /** Cierra la ventana. El evento close (que también dispara Esc) vuelve a la lista. */
  protected cerrar(): void {
    this.dialogo().nativeElement.close();
  }

  /** A la lista de estudiantes del plan, que es la ruta de arriba de 'nuevo'. */
  protected volver(): void {
    void this.router.navigate(['..'], { relativeTo: this.ruta });
  }
}

/** El mensaje del backend si lo hay (por ejemplo, "ya está inscripta"), o uno general. */
function mensajeDeError(error: unknown): string {
  if (error instanceof HttpErrorResponse && error.status !== 0 && error.status !== 500) {
    return error.error?.errores?.[0]?.mensaje ?? error.error?.detail ?? 'Ocurrió un error.';
  }
  return 'Ocurrió un error.';
}
