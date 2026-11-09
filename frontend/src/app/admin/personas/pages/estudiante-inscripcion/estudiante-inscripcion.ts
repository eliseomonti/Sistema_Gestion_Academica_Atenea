import { HttpErrorResponse } from '@angular/common/http';
import {
  afterNextRender,
  Component,
  ElementRef,
  inject,
  input,
  numberAttribute,
  signal,
  viewChild,
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { Icono } from '../../../../shared/ui/icono/icono';
import { EstudiantesApi } from '../../../estudiantes/data/estudiantes-api';
import { DatosPersonalesForm } from '../../components/datos-personales-form/datos-personales-form';
import { DniPaso } from '../../components/dni-paso/dni-paso';
import { AltaPersonaResponse, DatosPersonalesRequest, PersonaResponse } from '../../data/persona';
import { PersonasApi } from '../../data/personas-api';

/**
 * Ventana para inscribir a alguien en el plan, sobre la lista de estudiantes del plan (es una ruta
 * hija de la lista). Va por pasos, como el alta de docente. Primero se busca el DNI:
 * - Si la persona ya existe (estudiante de otra carrera, o docente), sólo se la inscribe.
 * - Si no existe, se cargan sus datos: el alta la deja inscripta y se muestran su usuario y su
 *   contraseña.
 */
@Component({
  selector: 'app-estudiante-inscripcion',
  imports: [DniPaso, DatosPersonalesForm, Icono],
  templateUrl: './estudiante-inscripcion.html',
  styleUrl: './estudiante-inscripcion.css',
})
export class EstudianteInscripcion {
  private readonly personasApi = inject(PersonasApi);
  private readonly estudiantesApi = inject(EstudiantesApi);
  private readonly router = inject(Router);
  private readonly ruta = inject(ActivatedRoute);

  /** El :planId de la URL. La carrera no hace falta: el backend la saca del plan. */
  readonly planId = input.required({ transform: numberAttribute });

  private readonly dialogo = viewChild.required<ElementRef<HTMLDialogElement>>('dialogo');

  /** El DNI buscado, cuando no es de nadie: pasa al paso de los datos personales. */
  protected readonly dniNuevo = signal<string | null>(null);
  protected readonly personaExistente = signal<PersonaResponse | null>(null);
  protected readonly credenciales = signal<AltaPersonaResponse | null>(null);
  protected readonly cargando = signal(false);
  protected readonly error = signal<string | null>(null);

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

  /** Alta de la persona nueva, que queda inscripta en el plan. */
  protected async guardar(datos: DatosPersonalesRequest): Promise<void> {
    this.error.set(null);
    this.cargando.set(true);
    try {
      const alta = this.estudiantesApi.alta({ datosPersonales: datos, planEstudioId: this.planId() });
      this.credenciales.set(await firstValueFrom(alta));
    } catch (error) {
      this.error.set(mensajeDeError(error));
    }
    this.cargando.set(false);
  }

  /** Inscribe en el plan a la persona que ya existía. */
  protected async inscribir(personaId: number): Promise<void> {
    this.error.set(null);
    this.cargando.set(true);
    try {
      await firstValueFrom(
        this.estudiantesApi.inscribir(personaId, { planEstudioId: this.planId() }),
      );
      this.cerrar();
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
