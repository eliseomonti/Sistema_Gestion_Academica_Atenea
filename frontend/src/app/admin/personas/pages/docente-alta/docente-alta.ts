import { HttpErrorResponse } from '@angular/common/http';
import {
  afterNextRender,
  Component,
  computed,
  ElementRef,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { Icono } from '../../../../shared/ui/icono/icono';
import { DocentesApi } from '../../../docentes/data/docentes-api';
import { DatosPersonalesForm } from '../../components/datos-personales-form/datos-personales-form';
import { DniPaso } from '../../components/dni-paso/dni-paso';
import { AltaPersonaResponse, DatosPersonalesRequest, PersonaResponse } from '../../data/persona';
import { PersonasApi } from '../../data/personas-api';

/**
 * Ventana para agregar un docente, sobre la lista de docentes (es una ruta hija de la lista). Va
 * por pasos, como la inscripción de estudiantes. Primero se busca el DNI:
 * - Si la persona ya existe (por ejemplo, un estudiante), se le da el rol de docente.
 * - Si ya es docente o está dada de baja, se avisa y no se puede seguir.
 * - Si no existe, se cargan sus datos: el alta crea el docente y muestra su usuario y su
 *   contraseña.
 */
@Component({
  selector: 'app-docente-alta',
  imports: [DniPaso, DatosPersonalesForm, Icono],
  templateUrl: './docente-alta.html',
  styleUrl: './docente-alta.css',
})
export class DocenteAlta {
  private readonly personasApi = inject(PersonasApi);
  private readonly docentesApi = inject(DocentesApi);
  private readonly router = inject(Router);
  private readonly ruta = inject(ActivatedRoute);

  private readonly dialogo = viewChild.required<ElementRef<HTMLDialogElement>>('dialogo');

  /** El DNI buscado, cuando no es de nadie: pasa al paso de los datos personales. */
  protected readonly dniNuevo = signal<string | null>(null);
  protected readonly personaExistente = signal<PersonaResponse | null>(null);
  protected readonly credenciales = signal<AltaPersonaResponse | null>(null);
  /** A quién se agregó, para el mensaje de éxito. Con esto la ventana pasa al último paso. */
  protected readonly agregado = signal<string | null>(null);
  protected readonly cargando = signal(false);
  protected readonly error = signal<string | null>(null);

  /**
   * Por qué la persona que ya existe no puede ser docente, o null si puede. Son las mismas reglas
   * que el backend (DocenteService.asignarRol), y los dos datos ya vienen en la búsqueda por DNI.
   */
  protected readonly motivo = computed(() => {
    const persona = this.personaExistente();
    if (!persona) {
      return null;
    }
    if (!persona.activo) {
      return 'La persona está dada de baja.';
    }
    if (persona.legajoDocente !== null) {
      return `Ya es docente, con el legajo ${persona.legajoDocente}.`;
    }
    return null;
  });

  /** La persona que ya existe y puede ser docente: se pregunta antes de darle el rol. */
  protected readonly aAsignar = computed(() => (this.motivo() ? null : this.personaExistente()));

  constructor() {
    // showModal() la abre como ventana: oscurece la lista y deja el foco del teclado adentro.
    afterNextRender(() => this.dialogo().nativeElement.showModal());
  }

  /** El backend responde 404 si nadie tiene ese DNI: en ese caso se pasa al alta. */
  protected async buscar(dni: string): Promise<void> {
    this.error.set(null);
    this.personaExistente.set(null);
    this.cargando.set(true);
    try {
      this.personaExistente.set(await firstValueFrom(this.personasApi.buscarPorDni(dni)));
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
  }

  /** Alta de la persona nueva como docente. */
  protected async guardar(datos: DatosPersonalesRequest): Promise<void> {
    this.error.set(null);
    this.cargando.set(true);
    try {
      this.credenciales.set(await firstValueFrom(this.docentesApi.altaDocente(datos)));
      this.agregado.set(`${datos.nombre} ${datos.apellido}`);
    } catch (error) {
      this.error.set(mensajeDeError(error));
    }
    this.cargando.set(false);
  }

  /** Le da el rol de docente a la persona que ya existía. Conserva su usuario y su contraseña. */
  protected async asignarRol(persona: PersonaResponse): Promise<void> {
    this.error.set(null);
    this.cargando.set(true);
    try {
      await firstValueFrom(this.docentesApi.asignarRol(persona.id));
      this.agregado.set(`${persona.nombre} ${persona.apellido}`);
    } catch (error) {
      this.error.set(mensajeDeError(error));
    }
    this.cargando.set(false);
  }

  /** Cierra la ventana. El evento close (que también dispara Esc) vuelve a la lista. */
  protected cerrar(): void {
    this.dialogo().nativeElement.close();
  }

  /** A la lista de docentes, que es la ruta de arriba de 'nuevo'. */
  protected volver(): void {
    void this.router.navigate(['..'], { relativeTo: this.ruta });
  }
}

/** El mensaje del backend si lo hay, o uno general. */
function mensajeDeError(error: unknown): string {
  if (error instanceof HttpErrorResponse && error.status !== 0 && error.status !== 500) {
    return error.error?.errores?.[0]?.mensaje ?? error.error?.detail ?? 'Ocurrió un error.';
  }
  return 'Ocurrió un error.';
}
