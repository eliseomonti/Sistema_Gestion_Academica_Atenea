import { HttpErrorResponse} from '@angular/common/http';
import { afterNextRender, Component, ElementRef, inject, input, numberAttribute, signal, viewChild } from '@angular/core';
import { ComisionesAPi } from '../../data/comisiones-api';
import { ActivatedRoute, Router } from '@angular/router';
import { Turno } from '../../../../shared/models/turno';
import { form, required, min, FormRoot, FormField } from '@angular/forms/signals';
import { firstValueFrom } from 'rxjs';
import { TurnoPipe } from '../../../../shared/pipes/turno-pipe';

@Component({
  imports: [FormRoot, FormField, TurnoPipe],
  selector: 'app-comision-alta',
  styleUrl: './comision-alta.css',
  templateUrl: './comision-alta.html',
})
export class ComisionAlta {

  private readonly api = inject(ComisionesAPi)
  private readonly router = inject(Router)
  private readonly ruta = inject(ActivatedRoute);

  private readonly dialogo = viewChild.required<ElementRef<HTMLDialogElement>>('dialogo')

  readonly planId = input.required({transform: numberAttribute})
  protected readonly anios = [1,2,3,4,5,6,7]
  protected readonly turnos: Turno[] = ['MANANA', 'MEDIA_MANANA', 'TARDE', 'NOCHE']
  protected readonly creada = signal<string | null>(null)
  protected readonly cargando = signal(false)
  protected readonly error = signal<string | null>(null)

  private readonly datos = signal({
    anioCursado: '',
    nroComision: 1,
    turno: '',
    anioLectivo: new Date().getFullYear(),
  })

  protected readonly comisionForm = form(
    this.datos, (campo) => {
      required(campo.anioCursado, {message: 'Elegi el año'});
      required(campo.turno, {message: 'Elegi el turno'});
      min(campo.nroComision, 1, {message: 'El numero tiene que ser mayor a 0'});
      min(campo.anioLectivo, 2000, { message: 'Revisá el año lectivo' });
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
    afterNextRender(() => this.dialogo().nativeElement.showModal())
  }

  private async guardar(): Promise<void>
  {
    this.error.set(null);
    this.cargando.set(true);
    try
    {
      const comision = await firstValueFrom(this.api.altaComision({
        planEstudioId: this.planId(),
        anioCursado: Number(this.datos().anioCursado),
        nroComision: this.datos().nroComision,
        turno: this.datos().turno as Turno,
        anioLectivo: this.datos().anioLectivo,
    }))
    this.creada.set(comision.nombre);
  }catch(error)
  {
    this.error.set(mensajeDeError(error))
  }
  this.cargando.set(false)

}

  protected cerrar(): void
  {
    this.dialogo().nativeElement.close();
  }

  protected volver(): void
  {
    void this.router.navigate(['..'],{relativeTo: this.ruta})
  }

}

function mensajeDeError(error: unknown): string
{
  if (error instanceof HttpErrorResponse && error.status !== 0 && error.status !== 500) {
    return error.error?.errores?.[0]?.mensaje ?? error.error?.detail ?? 'Ocurrió un error';
  }
  return 'Ocurrió un error';
}