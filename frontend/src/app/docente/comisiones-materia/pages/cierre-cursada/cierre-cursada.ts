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
  required,
  validate,
} from '@angular/forms/signals';
import { firstValueFrom } from 'rxjs';
import { Celda } from '../../../../shared/ui/tabla/celda';
import { Columna } from '../../../../shared/ui/tabla/columna';
import { Tabla } from '../../../../shared/ui/tabla/tabla';
import {
  CierreEstudianteResponse,
  CierreRequest,
  CONDICIONES_CURSADA,
  CondicionCursada,
} from '../../data/comision-materia';
import { ComisionesMateriaApi } from '../../data/comisiones-materia-api';

/**
 * El acta de cierre de la materia: cómo terminó la cursada de cada estudiante (regular,
 * promocionado o no regular) y su nota de cursada. Se completan todas y se guardan juntas. Es la
 * pestaña Cierre de ComisionMateriaDetalle.
 */
@Component({
  selector: 'app-cierre-cursada',
  imports: [FormRoot, FormField, Tabla, Celda],
  templateUrl: './cierre-cursada.html',
  styleUrl: './cierre-cursada.css',
})
export class CierreCursada {
  private readonly api = inject(ComisionesMateriaApi);

  readonly comisionMateriaId = input.required({ transform: numberAttribute });

  protected readonly acta = rxResource({
    params: () => this.comisionMateriaId(),
    stream: ({ params: comisionMateriaId }) => this.api.acta(comisionMateriaId),
  });

  protected readonly condiciones = Object.entries(CONDICIONES_CURSADA).map(([valor, texto]) => ({
    valor,
    texto,
  }));

  protected readonly columnas: Columna<CierreEstudianteResponse>[] = [
    { clave: 'legajo', titulo: 'Legajo', tituloEnLista: true },
    {
      clave: 'nombre',
      titulo: 'Estudiante',
      valor: (e) => `${e.apellido}, ${e.nombre}`,
      principal: true,
    },
    // Las dos las muestra un <ng-template appCelda>.
    { clave: 'condicion', titulo: 'Condición', tituloEnLista: true },
    { clave: 'nota', titulo: 'Nota de cursada', tituloEnLista: true },
  ];

  /** Lo que se edita: una fila por estudiante, en el mismo orden que el acta. '' es "sin cerrar". */
  private readonly datos = linkedSignal(() => ({
    filas: (this.acta.value()?.estudiantes ?? []).map((fila) => ({
      condicion: (fila.condicion ?? '') as CondicionCursada | '',
      nota: fila.nota,
    })),
  }));

  protected readonly actaForm = form(
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
        required(fila.nota, {
          message: 'La promoción lleva nota',
          when: ({ valueOf }) => valueOf(fila.condicion) === 'PROMOCIONADO',
        });
        // Sin condición no hay nada que cerrar: la nota se carga después de elegirla.
        disabled(fila.nota, ({ valueOf }) => valueOf(fila.condicion) === '');
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
      new Map(
        (this.acta.value()?.estudiantes ?? []).map((fila, indice) => [
          fila.inscripcionComisionId,
          indice,
        ]),
      ),
  );

  protected readonly guardando = signal(false);
  protected readonly mensaje = signal<string | null>(null);
  protected readonly error = signal<string | null>(null);

  protected fecha(fecha: string): string {
    return fecha.split('-').reverse().join('/');
  }

  /** Se mandan sólo las filas con condición que cambiaron. Una fila sin condición no se manda. */
  private async guardar(): Promise<void> {
    const acta = this.acta.value();
    if (!acta) {
      return;
    }
    const cierres: CierreRequest[] = [];
    this.datos().filas.forEach((fila, indice) => {
      const original = acta.estudiantes[indice];
      if (
        fila.condicion !== '' &&
        (fila.condicion !== original.condicion || fila.nota !== original.nota)
      ) {
        cierres.push({
          inscripcionComisionId: original.inscripcionComisionId,
          condicion: fila.condicion,
          nota: fila.nota,
        });
      }
    });

    this.mensaje.set(null);
    this.error.set(null);
    if (cierres.length === 0) {
      this.mensaje.set('No hay cambios para guardar.');
      return;
    }
    this.guardando.set(true);
    try {
      const actualizada = await firstValueFrom(
        this.api.cerrar(this.comisionMateriaId(), { cierres }),
      );
      // El acta nueva vuelve a armar los campos (datos es un linkedSignal de ella).
      this.acta.set(actualizada);
      this.mensaje.set(
        cierres.length === 1
          ? 'Se guardó el cierre de 1 estudiante.'
          : `Se guardó el cierre de ${cierres.length} estudiantes.`,
      );
    } catch (error) {
      this.error.set(mensajeDeError(error));
    }
    this.guardando.set(false);
  }
}

function mensajeDeError(error: unknown): string {
  if (error instanceof HttpErrorResponse && error.status !== 0 && error.status !== 500) {
    return error.error?.errores?.[0]?.mensaje ?? error.error?.detail ?? 'Ocurrió un error';
  }
  return 'Ocurrió un error';
}
