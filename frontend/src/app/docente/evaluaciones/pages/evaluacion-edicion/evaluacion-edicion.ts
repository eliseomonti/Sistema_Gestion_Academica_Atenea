import { HttpErrorResponse } from '@angular/common/http';
import {
  afterNextRender,
  Component,
  ElementRef,
  inject,
  input,
  linkedSignal,
  numberAttribute,
  signal,
  viewChild,
} from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { form, FormField, FormRoot, maxLength, required } from '@angular/forms/signals';
import { ActivatedRoute, Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { ConfirmarDialog } from '../../../../shared/ui/confirmar-dialog/confirmar-dialog';
import { Icono } from '../../../../shared/ui/icono/icono';
import { EvaluacionesApi } from '../../data/evaluaciones-api';

/**
 * Ventana para cambiar el nombre y la fecha de una evaluación, o eliminarla. El tipo y la
 * evaluación que recupera no cambian: para eso se elimina y se crea otra. Es hija de la planilla
 * de notas: se abre encima de ella.
 */
@Component({
  selector: 'app-evaluacion-edicion',
  imports: [FormRoot, FormField, ConfirmarDialog, Icono],
  templateUrl: './evaluacion-edicion.html',
  styleUrl: './evaluacion-edicion.css',
})
export class EvaluacionEdicion {
  private readonly api = inject(EvaluacionesApi);
  private readonly router = inject(Router);
  private readonly ruta = inject(ActivatedRoute);
  private readonly dialogo = viewChild.required<ElementRef<HTMLDialogElement>>('dialogo');

  readonly evaluacionId = input.required({ transform: numberAttribute });

  /** El nombre y la fecha actuales. No hay un endpoint para una sola evaluación: están en la planilla. */
  protected readonly evaluacion = rxResource({
    params: () => this.evaluacionId(),
    stream: ({ params: evaluacionId }) => this.api.notas(evaluacionId),
  });

  /** Con esto la ventana pasa a mostrar el mensaje de éxito. */
  protected readonly mensaje = signal<string | null>(null);
  protected readonly errorGuardar = signal<string | null>(null);
  protected readonly errorEliminar = signal<string | null>(null);
  protected readonly cargando = signal(false);
  /** Si se está mostrando la confirmación de eliminar. */
  protected readonly confirmando = signal(false);
  private eliminada = false;

  private readonly datos = linkedSignal(() => ({
    nombre: this.evaluacion.value()?.nombre ?? '',
    fecha: this.evaluacion.value()?.fecha ?? '',
  }));

  protected readonly evaluacionForm = form(
    this.datos,
    (campo) => {
      required(campo.nombre, { message: 'Escribí el nombre' });
      maxLength(campo.nombre, 50, { message: 'Puede tener hasta 50 caracteres' });
      required(campo.fecha, { message: 'Elegí la fecha' });
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

  private async guardar(): Promise<void> {
    const datos = this.datos();
    this.errorGuardar.set(null);
    this.cargando.set(true);
    try {
      await firstValueFrom(
        this.api.modificar(this.evaluacionId(), { nombre: datos.nombre.trim(), fecha: datos.fecha }),
      );
      this.mensaje.set('Se guardaron los cambios.');
    } catch (error) {
      this.errorGuardar.set(mensajeDeError(error));
    }
    this.cargando.set(false);
  }

  protected async eliminar(): Promise<void> {
    this.errorEliminar.set(null);
    this.cargando.set(true);
    try {
      await firstValueFrom(this.api.eliminar(this.evaluacionId()));
      this.eliminada = true;
      this.mensaje.set('Se eliminó la evaluación.');
    } catch (error) {
      // El mensaje del backend trae el id de la evaluación: se reemplaza por uno sin él.
      this.errorEliminar.set(
        error instanceof HttpErrorResponse && error.status === 409
          ? 'Tiene notas o recuperatorios cargados: no se puede eliminar.'
          : mensajeDeError(error),
      );
    }
    this.confirmando.set(false);
    this.cargando.set(false);
  }

  protected cerrar(): void {
    this.dialogo().nativeElement.close();
  }

  /** Al cerrarse vuelve a la planilla. Si se eliminó la evaluación, a la lista de evaluaciones. */
  protected volver(): void {
    void this.router.navigate([this.eliminada ? '../..' : '..'], { relativeTo: this.ruta });
  }
}

/** El mensaje del backend ("Ya hay una evaluación Parcial 1..."), o uno genérico. */
function mensajeDeError(error: unknown): string {
  if (error instanceof HttpErrorResponse && error.status !== 0 && error.status !== 500) {
    return error.error?.errores?.[0]?.mensaje ?? error.error?.detail ?? 'Ocurrió un error';
  }
  return 'Ocurrió un error';
}
