import { HttpErrorResponse } from '@angular/common/http';
import {
  Component,
  computed,
  inject,
  input,
  linkedSignal,
  numberAttribute,
  signal,
} from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import {
  applyEach,
  disabled,
  form,
  FormField,
  FormRoot,
  max,
  min,
  validate,
} from '@angular/forms/signals';
import { RouterLink, RouterOutlet } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { Icono } from '../../../../shared/ui/icono/icono';
import { Celda } from '../../../../shared/ui/tabla/celda';
import { Columna } from '../../../../shared/ui/tabla/columna';
import { Tabla } from '../../../../shared/ui/tabla/tabla';
import { TIPOS_EVALUACION } from '../../../comisiones-materia/data/comision-materia';
import { NotaEstudianteResponse, NotaRequest } from '../../data/evaluacion';
import { EvaluacionesApi } from '../../data/evaluaciones-api';

/**
 * La planilla de notas de una evaluación: todos los anotados a la materia, por apellido, cada uno
 * con su nota o "Ausente". Se cargan todas y se guardan juntas. Se abre desde la pestaña
 * Evaluaciones de la materia, debajo de las pestañas, que siguen a la vista.
 */
@Component({
  selector: 'app-evaluacion-notas',
  imports: [RouterLink, RouterOutlet, FormRoot, FormField, Icono, Tabla, Celda],
  templateUrl: './evaluacion-notas.html',
  styleUrl: './evaluacion-notas.css',
})
export class EvaluacionNotas {
  private readonly api = inject(EvaluacionesApi);

  readonly evaluacionId = input.required({ transform: numberAttribute });

  protected readonly planilla = rxResource({
    params: () => this.evaluacionId(),
    stream: ({ params: evaluacionId }) => this.api.notas(evaluacionId),
  });

  protected readonly tipos = TIPOS_EVALUACION;

  protected readonly columnas: Columna<NotaEstudianteResponse>[] = [
    { clave: 'legajo', titulo: 'Legajo', tituloEnLista: true },
    {
      clave: 'nombre',
      titulo: 'Estudiante',
      valor: (e) => `${e.apellido}, ${e.nombre}`,
      principal: true,
    },
    { clave: 'nota', titulo: 'Nota', tituloEnLista: true }, // lo muestra <ng-template appCelda>
  ];

  /** El backend no acepta notas antes del día de la evaluación: hasta entonces, sólo se ve. */
  protected readonly antesDeLaFecha = computed(() => {
    const planilla = this.planilla.value();
    return planilla ? planilla.fecha > hoy() : false;
  });

  /** Lo que se edita: una fila por estudiante, en el mismo orden que la planilla. */
  private readonly datos = linkedSignal(() => ({
    filas: (this.planilla.value()?.notas ?? []).map((fila) => ({
      ausente: fila.condicion === 'AUSENTE',
      nota: fila.nota,
    })),
  }));

  protected readonly notasForm = form(
    this.datos,
    (campo) => {
      applyEach(campo.filas, (fila) => {
        min(fila.nota, 0, { message: 'Entre 0 y 10' });
        max(fila.nota, 10, { message: 'Entre 0 y 10' });
        validate(fila.nota, ({ value }) => {
          const nota = value();
          return nota !== null && !Number.isInteger(nota)
            ? { kind: 'entero', message: 'Sin decimales' }
            : undefined;
        });
        // Un ausente no lleva nota.
        disabled(fila.nota, ({ valueOf }) => valueOf(fila.ausente) || this.antesDeLaFecha());
        disabled(fila.ausente, () => this.antesDeLaFecha());
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

  /** Para encontrar el campo de cada fila de la tabla: están en el mismo orden. */
  protected readonly indices = computed(
    () =>
      new Map((this.planilla.value()?.notas ?? []).map((fila, indice) => [fila.inscripcionId, indice])),
  );

  protected readonly guardando = signal(false);
  protected readonly mensaje = signal<string | null>(null);
  protected readonly error = signal<string | null>(null);

  protected fecha(fecha: string): string {
    return fecha.split('-').reverse().join('/');
  }

  /**
   * Se mandan sólo las filas que cambiaron: si una cursada ya tiene acta de cierre, el backend
   * rechaza todo el pedido aunque su nota venga igual. Una fila vacía no se manda.
   */
  private async guardar(): Promise<void> {
    const planilla = this.planilla.value();
    if (!planilla) {
      return;
    }
    const notas: NotaRequest[] = [];
    this.datos().filas.forEach((fila, indice) => {
      const original = planilla.notas[indice];
      const inscripcionId = original.inscripcionId;
      if (fila.ausente && original.condicion !== 'AUSENTE') {
        notas.push({ inscripcionId, condicion: 'AUSENTE', nota: null });
      } else if (
        !fila.ausente &&
        fila.nota !== null &&
        (original.condicion !== 'PRESENTE' || original.nota !== fila.nota)
      ) {
        notas.push({ inscripcionId, condicion: 'PRESENTE', nota: fila.nota });
      }
    });

    this.mensaje.set(null);
    this.error.set(null);
    if (notas.length === 0) {
      this.mensaje.set('No hay notas nuevas para guardar.');
      return;
    }
    this.guardando.set(true);
    try {
      const actualizada = await firstValueFrom(this.api.cargarNotas(planilla.evaluacionId, { notas }));
      // La planilla nueva vuelve a armar los campos (datos es un linkedSignal de ella).
      this.planilla.set(actualizada);
      this.mensaje.set(notas.length === 1 ? 'Se guardó 1 nota.' : `Se guardaron ${notas.length} notas.`);
    } catch (error) {
      this.error.set(mensajeDeError(error));
    }
    this.guardando.set(false);
  }
}

/** "2026-05-14", en la fecha de la compu: el mismo formato que las fechas del backend. */
function hoy(): string {
  const ahora = new Date();
  const mes = String(ahora.getMonth() + 1).padStart(2, '0');
  const dia = String(ahora.getDate()).padStart(2, '0');
  return `${ahora.getFullYear()}-${mes}-${dia}`;
}

/** El mensaje del backend ("La cursada de la inscripción... ya tiene acta"), o uno genérico. */
function mensajeDeError(error: unknown): string {
  if (error instanceof HttpErrorResponse && error.status !== 0 && error.status !== 500) {
    return error.error?.errores?.[0]?.mensaje ?? error.error?.detail ?? 'Ocurrió un error';
  }
  return 'Ocurrió un error';
}
