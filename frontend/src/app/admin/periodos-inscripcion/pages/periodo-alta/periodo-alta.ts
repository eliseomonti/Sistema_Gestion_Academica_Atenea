import { afterNextRender, Component, ElementRef, inject, input, numberAttribute, signal, viewChild} from '@angular/core';
import { PeriodoInscripcionApi } from '../../data/periodos-inscripcion-api';
import { ActivatedRoute, Router } from '@angular/router';
import { form, required, min, FormField, FormRoot} from '@angular/forms/signals';
import { firstValueFrom } from 'rxjs';
import { TipoPeriodo } from '../../data/periodo-inscripcion';
import { PeriodoLectivo } from '../../../carreras/data/carrera';
import { HttpErrorResponse } from '@angular/common/http';


@Component({
  imports: [FormField, FormRoot],
  selector: 'app-periodo-alta',
  styleUrl: './periodo-alta.css',
  templateUrl: './periodo-alta.html',
})
export class PeriodoAlta {

  private readonly api = inject(PeriodoInscripcionApi)
  private readonly router = inject(Router)
  private readonly ruta = inject(ActivatedRoute)
  private readonly dialogo = viewChild.required<ElementRef<HTMLDialogElement>>('dialogo');

  readonly carreraId = input.required({transform: numberAttribute})

  protected readonly creado = signal(false)
  protected readonly cargando = signal(false)
  protected readonly error = signal<string | null>(null)

  private readonly datos = signal({
    tipo: '',
    anioLectivo: new Date().getFullYear(),
    periodoLectivo: '',
    fechaInicio: '',
    fechaCierre: ''
  })

  protected readonly periodoForm = form(
    this.datos, (campo) => {
      required(campo.tipo, {message: 'Elegio el tipo'});
      min(campo.anioLectivo, 2000, { message: 'Revisa el año lectivo' });

      required(campo.periodoLectivo, {message: 'Elegi el cuatrimestre', when: ({ valueOf }) => valueOf(campo.tipo) === 'CURSADA'});
      required(campo.fechaInicio, {message: 'Elegi la fecha de inicio'});
      required(campo.fechaCierre, {message: 'Elegi la fecha de cierre'});

    },
    {
      submission: {
        action: async () => {
          await this.guardar();
          return undefined;
        }
      }
    }
  );

  constructor()
  {
    afterNextRender(() => this.dialogo().nativeElement.showModal());
  }

  private async guardar(): Promise<void>
  {
    const datos = this.datos();
    this.error.set(null);
    this.cargando.set(true);
    try {
      await firstValueFrom(this.api.altaPeriodo({
        carreraId: this.carreraId(),
        tipo: datos.tipo as TipoPeriodo,
        anioLectivo: datos.anioLectivo,
        periodoLectivo: datos.tipo === 'CURSADA' ? (datos.periodoLectivo as PeriodoLectivo) : null,
        fechaInicio: datos.fechaInicio,
        fechaCierre: datos.fechaCierre,
      }));
      this.creado.set(true);
    } catch (error) {
      this.error.set(mensajeDeError(error));
    }
    this.cargando.set(false);
  }

  protected cerrar(): void
  {
    this.dialogo().nativeElement.close();
  }

  protected volver(): void
  {
    void this.router.navigate(['..'], { relativeTo: this.ruta });
  }



}

function mensajeDeError(error: unknown): string
{
  if (error instanceof HttpErrorResponse && error.status !== 0 && error.status !== 500) {
    return error.error?.errores?.[0]?.mensaje ?? error.error?.detail ?? 'Ocurrió un error';
  }
  return 'Ocurrió un error';
}
