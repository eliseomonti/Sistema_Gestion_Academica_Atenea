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
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { Icono } from '../../../../shared/ui/icono/icono';
import { Celda } from '../../../../shared/ui/tabla/celda';
import { Columna } from '../../../../shared/ui/tabla/columna';
import { Tabla } from '../../../../shared/ui/tabla/tabla';
import { NotaEstudianteResponse, NotaRequest } from '../../../evaluaciones/data/evaluacion';
import { ExamenesFinalesApi } from '../../data/examenes-finales-api';

/**
 * La planilla de notas de una mesa de examen: los anotados, por apellido, cada uno con su nota o
 * "Ausente". Se cargan todas y se guardan juntas. Funciona igual que la planilla de una evaluación
 * (EvaluacionNotas).
 */
@Component({
  selector: 'app-examen-final-notas',
  imports: [RouterLink, FormRoot, FormField, Icono, Tabla, Celda],
  templateUrl: './examen-final-notas.html',
  styleUrl: './examen-final-notas.css',
})
export class ExamenFinalNotas {
  private readonly api = inject(ExamenesFinalesApi);

  readonly examenFinalId = input.required({ transform: numberAttribute });

  protected readonly planilla = rxResource({
    params: () => this.examenFinalId(),
    stream: ({ params: examenFinalId }) => this.api.notas(examenFinalId),
  });

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

  /** El backend no acepta notas antes del día de la mesa: hasta entonces, sólo se ve. */
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

  /** Se mandan sólo las filas que cambiaron. Una fila vacía no se manda. */
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
      const actualizada = await firstValueFrom(
        this.api.cargarNotas(planilla.examenFinalId, { notas }),
      );
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

/** El mensaje del backend, o uno genérico. */
function mensajeDeError(error: unknown): string {
  if (error instanceof HttpErrorResponse && error.status !== 0 && error.status !== 500) {
    return error.error?.errores?.[0]?.mensaje ?? error.error?.detail ?? 'Ocurrió un error';
  }
  return 'Ocurrió un error';
}
