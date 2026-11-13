import { HttpErrorResponse } from '@angular/common/http';
import {
  afterNextRender,
  Component,
  computed,
  ElementRef,
  inject,
  input,
  numberAttribute,
  signal,
  viewChild,
} from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { form, FormField, FormRoot, maxLength, required } from '@angular/forms/signals';
import { ActivatedRoute, Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { Icono } from '../../../../shared/ui/icono/icono';
import { TipoEvaluacion, TIPOS_EVALUACION } from '../../data/comision-materia';
import { ComisionesMateriaApi } from '../../data/comisiones-materia-api';

/**
 * Ventana para cargar una evaluación de la materia, o el recuperatorio de otra. Es hija de la
 * lista de evaluaciones: se abre encima de ella y, al cerrarse, vuelve a la lista.
 */
@Component({
  selector: 'app-evaluacion-alta',
  imports: [FormRoot, FormField, Icono],
  templateUrl: './evaluacion-alta.html',
  styleUrl: './evaluacion-alta.css',
})
export class EvaluacionAlta {
  private readonly api = inject(ComisionesMateriaApi);
  private readonly router = inject(Router);
  private readonly ruta = inject(ActivatedRoute);
  private readonly dialogo = viewChild.required<ElementRef<HTMLDialogElement>>('dialogo');

  readonly comisionMateriaId = input.required({ transform: numberAttribute });

  protected readonly tipos = Object.entries(TIPOS_EVALUACION).map(([valor, texto]) => ({
    valor,
    texto,
  }));

  private readonly evaluaciones = rxResource({
    params: () => this.comisionMateriaId(),
    stream: ({ params: comisionMateriaId }) => this.api.listarEvaluaciones(comisionMateriaId),
  });

  /** Las que se pueden recuperar: las que no son a su vez un recuperatorio. */
  protected readonly recuperables = computed(() =>
    (this.evaluaciones.value() ?? []).filter((e) => e.evaluacionRecuperadaId === null),
  );

  /** El nombre de la evaluación creada: con esto la ventana pasa a mostrar el mensaje de éxito. */
  protected readonly creada = signal<string | null>(null);
  protected readonly cargando = signal(false);
  protected readonly error = signal<string | null>(null);

  // Los <select> trabajan con texto: '' es "sin elegir".
  private readonly datos = signal({
    nombre: '',
    fecha: '',
    recuperatorio: false,
    tipo: '',
    evaluacionRecuperadaId: '',
  });

  /** Un recuperatorio no lleva tipo, sino la evaluación que recupera: se pide uno u otro. */
  protected readonly evaluacionForm = form(
    this.datos,
    (campo) => {
      required(campo.nombre, { message: 'Escribí el nombre' });
      maxLength(campo.nombre, 50, { message: 'Puede tener hasta 50 caracteres' });
      required(campo.fecha, { message: 'Elegí la fecha' });
      required(campo.tipo, {
        message: 'Elegí el tipo',
        when: ({ valueOf }) => !valueOf(campo.recuperatorio),
      });
      required(campo.evaluacionRecuperadaId, {
        message: 'Elegí qué evaluación recupera',
        when: ({ valueOf }) => valueOf(campo.recuperatorio),
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

  private async guardar(): Promise<void> {
    const datos = this.datos();
    this.error.set(null);
    this.cargando.set(true);
    try {
      const evaluacion = await firstValueFrom(
        this.api.altaEvaluacion(this.comisionMateriaId(), {
          nombre: datos.nombre.trim(),
          fecha: datos.fecha,
          tipo: datos.recuperatorio ? null : (datos.tipo as TipoEvaluacion),
          evaluacionRecuperadaId: datos.recuperatorio ? Number(datos.evaluacionRecuperadaId) : null,
        }),
      );
      this.creada.set(evaluacion.nombre);
    } catch (error) {
      this.error.set(mensajeDeError(error));
    }
    this.cargando.set(false);
  }

  protected cerrar(): void {
    this.dialogo().nativeElement.close();
  }

  /** Al cerrarse la ventana (✕, Cancelar, Listo o Esc), vuelve a la lista. */
  protected volver(): void {
    void this.router.navigate(['..'], { relativeTo: this.ruta });
  }
}

/** El mensaje del backend ("Ya hay una evaluación Parcial 1..."), o uno genérico. */
function mensajeDeError(error: unknown): string {
  if (error instanceof HttpErrorResponse && error.status !== 0 && error.status !== 500) {
    return error.error?.errores?.[0]?.mensaje ?? error.error?.detail ?? 'Ocurrió un error';
  }
  return 'Ocurrió un error';
}
